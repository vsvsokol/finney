package ru.finney.pet.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import ru.finney.pet.domain.model.BodyColor
import ru.finney.pet.domain.model.Category
import ru.finney.pet.domain.model.EntryType
import ru.finney.pet.domain.model.EyesVariant
import ru.finney.pet.domain.model.PetCharacter
import ru.finney.pet.domain.model.PeriodPhase
import ru.finney.pet.domain.model.TaskOutcome

// В базе только факты. Баланс, накопления, уровень и эмоция считаются в domain (docs/architecture.md).
// Все дочерние таблицы удаляются вместе с профилем (CASCADE) — сброс профиля одной командой.

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val petName: String,
    val petCharacter: PetCharacter,
    val bodyColor: BodyColor,
    val eyes: EyesVariant,
    val activeGoalId: String?,
    val isDemo: Boolean,
    val createdAt: Long,
    /** Надетый аксессуар. Колонка добавлена в версии 4 базы, см. [FinneyDatabase.MIGRATION_3_4]. */
    val wornItemId: String? = null,
)

@Entity(
    tableName = "pet_state",
    foreignKeys = [ForeignKey(ProfileEntity::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE)],
)
data class PetStateEntity(
    @PrimaryKey val profileId: Long,
    val satiety: Int,
    val hygiene: Int,
    val mood: Int,
)

@Entity(
    tableName = "periods",
    primaryKeys = ["profileId", "number"],
    foreignKeys = [ForeignKey(ProfileEntity::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE)],
)
data class PeriodEntity(
    val profileId: Long,
    val number: Int,
    val stage: Int,
    val phase: PeriodPhase,
    // план — заполнен после подтверждения
    val budget: Int?,
    val plannedNeeds: Int?,
    val plannedWants: Int?,
    val plannedSavings: Int?,
    // итоги — заполнены после закрытия
    val factNeeds: Int?,
    val factWants: Int?,
    val factSavings: Int?,
    val unplannedIncome: Int?,
    val needsCovered: Boolean?,
    val planMatched: Boolean?,
    val savingsAdded: Boolean?,
    val successfulTasks: Int?,
    val pointsEarned: Int?,
)

@Entity(
    tableName = "ledger",
    foreignKeys = [ForeignKey(ProfileEntity::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("profileId")],
)
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val periodNumber: Int,
    val type: EntryType,
    val balanceDelta: Int,
    val savingsDelta: Int,
    val category: Category?,
    val itemId: String?,
    val goalId: String?,
    val taskId: String?,
    val unplanned: Boolean,
    val createdAt: Long,
)

@Entity(
    tableName = "task_attempts",
    foreignKeys = [ForeignKey(ProfileEntity::class, ["id"], ["profileId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("profileId")],
)
data class TaskAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val periodNumber: Int,
    val taskId: String,
    val outcome: TaskOutcome,
    val reward: Int,
    val createdAt: Long,
)

/** Профиль со всем состоянием одним запросом. Порядок в списках Room не гарантирует — сортирует маппер. */
data class ProfileWithGame(
    @Embedded val profile: ProfileEntity,
    @Relation(parentColumn = "id", entityColumn = "profileId") val pet: PetStateEntity?,
    @Relation(parentColumn = "id", entityColumn = "profileId") val periods: List<PeriodEntity>,
    @Relation(parentColumn = "id", entityColumn = "profileId") val ledger: List<LedgerEntryEntity>,
    @Relation(parentColumn = "id", entityColumn = "profileId") val attempts: List<TaskAttemptEntity>,
)
