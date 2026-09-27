package ru.finney.pet.domain.tasks

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import ru.finney.pet.domain.model.TaskDefinition
import kotlin.random.Random

/**
 * Задание из tasks.json с разбросом. В шаблоне вместо значения можно написать:
 * - `{"min": 6, "max": 10}` — целое от 6 до 10, с `"step": 2` — через 2 (6, 8, 10);
 * - `{"oneOf": [a, b, c]}` — одно из значений;
 * - `{"pick": 2, "from": [a, b, c]}` — два из списка, порядок как в списке.
 *
 * В строках — подстановки: `{guests}`, `{ingredient.price}`, `{rounds.0.price}` берут
 * уже выбранное значение из этого же задания, а имена из [TaskEngines.facts] — числа,
 * которые считает движок («сколько лимонов купить»).
 *
 * [generate] превращает шаблон в обычное [TaskDefinition]: одно зерно — одни и те же
 * числа, поэтому попытку можно повторить и проверить. Экран получает готовые числа.
 */
object TaskGenerator {

    private val json = Json { ignoreUnknownKeys = false }

    /** Сколько раз пробовать новые числа, прежде чем взять [base]. */
    private const val TRIES = 30

    /** Поля, по которым задание узнают до генерации: они не бывают случайными. */
    private val FIXED = listOf("id", "engine", "theme", "series", "unlockLevel", "unlockPeriod")

    private val PLACEHOLDER = Regex("""\{([A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z0-9_]+)*)\}""")

    /**
     * Вариант без случайности: разброс — минимум, `oneOf` — первое, `pick` — первые.
     * Его показывают списки игр, и он же запасной, если случайные числа не сложились.
     * Ошибка в шаблоне — [IllegalArgumentException] с понятным текстом.
     */
    fun base(template: JsonObject): TaskDefinition {
        FIXED.filter { template[it]?.let(::isRandom) == true }.forEach {
            throw IllegalArgumentException("поле $it не может быть разбросом")
        }
        return build(template, random = null)
    }

    /**
     * Задание с числами зерна [seed]. Вариант, который не проходит [TaskChecks] (игру не
     * выиграть), отбрасывается, и берутся следующие числа того же зерна. За [TRIES] попыток
     * не сложилось — [base].
     */
    fun generate(template: JsonObject, seed: Long): TaskDefinition {
        if (!isRandom(template)) return base(template)
        val random = Random(seed)
        repeat(TRIES) {
            val task = build(template, random)
            if (TaskChecks.problems(task).isEmpty()) return task
        }
        return base(template)
    }

    /** Есть ли в [element] хоть один разброс. Без разброса задание не зависит от зерна. */
    fun isRandom(element: JsonElement): Boolean = when (element) {
        is JsonObject -> spreadKind(element) != null || element.values.any(::isRandom)
        is JsonArray -> element.any(::isRandom)
        is JsonPrimitive -> false
    }

    private fun build(template: JsonObject, random: Random?): TaskDefinition {
        val resolved = resolve(template, random) as JsonObject
        val facts = TaskEngines.facts(json.decodeFromJsonElement<TaskDefinition>(resolved))
        return json.decodeFromJsonElement<TaskDefinition>(substitute(resolved, resolved, facts))
    }

    // ---------- Разброс ----------

    private enum class Spread { RANGE, ONE_OF, PICK }

    private fun spreadKind(o: JsonObject): Spread? = when (o.keys) {
        setOf("min", "max"), setOf("min", "max", "step") -> Spread.RANGE
        setOf("oneOf") -> Spread.ONE_OF
        setOf("pick", "from") -> Spread.PICK
        else -> null
    }

    private fun resolve(element: JsonElement, random: Random?): JsonElement = when (element) {
        is JsonPrimitive -> element
        is JsonArray -> JsonArray(element.map { resolve(it, random) })
        is JsonObject -> when (spreadKind(element)) {
            Spread.RANGE -> JsonPrimitive(range(element, random))
            Spread.ONE_OF -> {
                val options = list(element, "oneOf")
                require(options.isNotEmpty()) { "в oneOf пустой список" }
                resolve(options[random?.nextInt(options.size) ?: 0], random)
            }
            Spread.PICK -> {
                val from = list(element, "from")
                val count = int(element, "pick")
                require(count in 1..from.size) { "pick должен быть от 1 до ${from.size}" }
                val indices = random?.let { from.indices.shuffled(it).take(count).sorted() } ?: (0 until count)
                JsonArray(indices.map { resolve(from[it], random) })
            }
            null -> JsonObject(element.mapValues { (_, v) -> resolve(v, random) })
        }
    }

    private fun range(o: JsonObject, random: Random?): Int {
        val min = int(o, "min")
        val max = int(o, "max")
        val step = if ("step" in o) int(o, "step") else 1
        require(step > 0) { "в разбросе $o step должен быть > 0" }
        require(max >= min) { "в разбросе $o max меньше min" }
        require((max - min) % step == 0) { "в разбросе $o от min до max не целое число шагов step" }
        return min + step * (random?.nextInt((max - min) / step + 1) ?: 0)
    }

    private fun int(o: JsonObject, key: String): Int =
        (o[key] as? JsonPrimitive)?.takeIf { !it.isString }?.intOrNull
            ?: throw IllegalArgumentException("в разбросе $o поле $key должно быть целым числом")

    private fun list(o: JsonObject, key: String): JsonArray =
        o[key] as? JsonArray ?: throw IllegalArgumentException("в разбросе $o поле $key должно быть списком")

    // ---------- Подстановки в текстах ----------

    private fun substitute(element: JsonElement, root: JsonObject, facts: Map<String, Int>): JsonElement = when (element) {
        is JsonObject -> JsonObject(element.mapValues { (_, v) -> substitute(v, root, facts) })
        is JsonArray -> JsonArray(element.map { substitute(it, root, facts) })
        is JsonPrimitive -> if (element.isString && '{' in element.content) {
            JsonPrimitive(PLACEHOLDER.replace(element.content) { lookup(it.groupValues[1], root, facts) })
        } else {
            element
        }
    }

    /** Сначала путь в задании, потом число движка: поле задания нельзя подменить фактом. */
    private fun lookup(path: String, root: JsonObject, facts: Map<String, Int>): String {
        var at: JsonElement? = root
        for (key in path.split('.')) {
            at = when (val node = at) {
                is JsonObject -> node[key]
                is JsonArray -> key.toIntOrNull()?.let(node::getOrNull)
                else -> null
            }
        }
        (at as? JsonPrimitive)?.let { return it.content }
        facts[path]?.let { return it.toString() }
        throw IllegalArgumentException("в тексте {$path}: такого поля в задании нет")
    }
}
