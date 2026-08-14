package com.example.kharjyar

enum class EntryType(val titleFa: String) {
    EXPENSE("هزینه"),
    INCOME("درآمد")
}

data class LedgerEntry(
    val id: Long = 0L,
    val type: EntryType,
    val amount: Long,
    val category: String,
    val subcategory: String = "",
    val tags: List<String> = emptyList(),
    val note: String = "",
    val occurredAt: Long,
    val createdAt: Long = System.currentTimeMillis()
)

data class Debt(
    val id: Long = 0L,
    val name: String,
    val originalAmount: Long,
    val currentAmount: Long,
    val note: String = "",
    val occurredAt: Long,
    val updatedAt: Long = System.currentTimeMillis()
)

data class DebtSnapshot(
    val id: Long = 0L,
    val debtId: Long,
    val amount: Long,
    val occurredAt: Long
)

data class CategoryRow(
    val id: Long = 0L,
    val type: EntryType,
    val name: String,
    val subcategory: String = ""
)

data class MonthRef(
    val year: Int,
    val month: Int,
    val label: String
) {
    val key: String get() = "%04d-%02d".format(year, month)
}

data class ChartBar(
    val label: String,
    val value: Long
)

data class ChartGroup(
    val label: String,
    val bars: List<ChartBar>
)
