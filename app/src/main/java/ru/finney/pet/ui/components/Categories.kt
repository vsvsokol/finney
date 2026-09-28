package ru.finney.pet.ui.components

import ru.finney.pet.domain.model.Category

// Три направления плана — нужное, желаемое, копилка — различаются значком, а не цветом.
// Плейтест: «почему нужное жёлтым, а копилка зелёным?» — ребёнок искал смысл в цвете,
// которого там не было, а зелёный у нас значит «получилось». Поэтому значок один на всё
// приложение и берётся отсюда: знакомство, план, магазин, итоги уровня, мини-игры.

/** Значок категории: вилка у нужного (еда — первое нужное), звезда у желаемого. */
fun categoryIcon(category: Category): FinneyIcons = when (category) {
    Category.NEEDS -> FinneyIcons.Food
    Category.WANTS -> FinneyIcons.Star
}

/** Значок копилки — третьего направления плана. Копилка не категория товара, отсюда отдельно. */
val SavingsIcon: FinneyIcons = FinneyIcons.Piggy
