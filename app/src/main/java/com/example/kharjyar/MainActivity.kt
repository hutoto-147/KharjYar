package com.example.kharjyar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val IncomeSoft = Color(0xFFE2F4E8)
private val ExpenseSoft = Color(0xFFFBE2E7)
private val BalanceSoft = Color(0xFFE1ECFA)
private val DebtSoft = Color(0xFFFFF0D6)
private val IncomeStrong = Color(0xFF4E9B6A)
private val ExpenseStrong = Color(0xFFD87584)
private val DebtStrong = Color(0xFFD49A38)

private data class VisualTheme(
    val id: String,
    val title: String,
    val top: Color,
    val bottom: Color,
    val surface: Color,
    val nav: Color,
    val primary: Color
)

private val visualThemes = listOf(
    VisualTheme(
        id = "lavender",
        title = "یاسی روشن",
        top = Color(0xFFFFFAFF),
        bottom = Color(0xFFF4EFFA),
        surface = Color(0xFFF5F0F7),
        nav = Color(0xFFF0EAF7),
        primary = Color(0xFF7254B8)
    ),
    VisualTheme(
        id = "mist",
        title = "آبی مه‌آلود",
        top = Color(0xFFFBFDFF),
        bottom = Color(0xFFECF5FB),
        surface = Color(0xFFF0F6FA),
        nav = Color(0xFFE8F1F8),
        primary = Color(0xFF4F7998)
    ),
    VisualTheme(
        id = "mint",
        title = "سبز خیلی روشن",
        top = Color(0xFFFCFFFD),
        bottom = Color(0xFFEDF7F1),
        surface = Color(0xFFF0F7F3),
        nav = Color(0xFFE8F2EC),
        primary = Color(0xFF507E63)
    )
)

private data class ChartPalette(
    val id: String,
    val title: String,
    val income: Color,
    val expense: Color,
    val other: Color
)

private val chartPalettes = listOf(
    ChartPalette("green_red", "سبز ـ قرمز", Color(0xFF65A97C), Color(0xFFD97C89), Color(0xFF7186B7)),
    ChartPalette("blue_yellow", "آبی ـ زرد", Color(0xFF5B8FBE), Color(0xFFD7AC54), Color(0xFF8A77B8)),
    ChartPalette("purple_teal", "بنفش ـ فیروزه‌ای", Color(0xFF8468B4), Color(0xFF58A3A0), Color(0xFFD08B67))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LedgerApp() }
    }
}

@Composable
private fun KharjYarTheme(theme: VisualTheme, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = theme.primary,
            surface = theme.surface,
            surfaceVariant = theme.surface,
            background = theme.bottom
        ),
        typography = Typography(),
        content = content
    )
}

private data class BottomTab(val title: String, val icon: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerApp() {
    val context = LocalContext.current
    val repo = remember { LedgerRepository(context) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var refreshToken by remember { mutableIntStateOf(0) }
    var editingEntry by remember { mutableStateOf<LedgerEntry?>(null) }
    var editingDebt by remember { mutableStateOf<Debt?>(null) }

    val themeId = remember(refreshToken) { repo.setting("visual_theme", "lavender") }
    val theme = visualThemes.firstOrNull { it.id == themeId } ?: visualThemes.first()

    val tabs = listOf(
        BottomTab("خانه", "⌂"),
        BottomTab("تراکنش‌ها", "≡"),
        BottomTab("ثبت", "＋"),
        BottomTab("مقایسه", "▥"),
        BottomTab("تنظیمات", "⚙")
    )

    val title = when {
        selectedTab == 2 && editingDebt != null -> "به‌روزرسانی بدهی"
        selectedTab == 2 && editingEntry != null -> "ویرایش تراکنش"
        selectedTab == 0 -> "خرج‌یار"
        selectedTab == 1 -> "تراکنش‌ها"
        selectedTab == 2 -> "ثبت"
        selectedTab == 3 -> "مقایسه"
        else -> "تنظیمات"
    }

    KharjYarTheme(theme) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(theme.top, theme.bottom)))
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text(title) },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = Color.Transparent
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(containerColor = theme.nav) {
                            tabs.forEachIndexed { index, tab ->
                                NavigationBarItem(
                                    selected = selectedTab == index,
                                    onClick = {
                                        if (index != 2) {
                                            editingEntry = null
                                            editingDebt = null
                                        }
                                        selectedTab = index
                                    },
                                    icon = { Text(tab.icon, fontSize = 20.sp) },
                                    label = { Text(tab.title, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (selectedTab) {
                            0 -> DashboardScreen(repo, refreshToken)
                            1 -> TransactionsScreen(
                                repo = repo,
                                refreshToken = refreshToken,
                                onEdit = { entry ->
                                    editingEntry = entry
                                    editingDebt = null
                                    selectedTab = 2
                                },
                                onEditDebt = { debt ->
                                    editingDebt = debt
                                    editingEntry = null
                                    selectedTab = 2
                                },
                                onDeleted = { refreshToken++ }
                            )
                            2 -> AddEntryScreen(
                                repo = repo,
                                refreshToken = refreshToken,
                                editing = editingEntry,
                                editingDebt = editingDebt,
                                onSaved = {
                                    refreshToken++
                                    editingEntry = null
                                    editingDebt = null
                                    selectedTab = 1
                                },
                                onCancelEdit = {
                                    editingEntry = null
                                    editingDebt = null
                                    selectedTab = 1
                                }
                            )
                            3 -> ComparisonScreen(repo, refreshToken)
                            4 -> SettingsScreen(
                                repo = repo,
                                refreshToken = refreshToken,
                                onChanged = { refreshToken++ }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Start,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun DashboardScreen(repo: LedgerRepository, refreshToken: Int) {
    val entries = remember(refreshToken) { repo.entries() }
    val budget = remember(refreshToken) { repo.budget() }
    val currentParts = PersianDate.parts(System.currentTimeMillis())
    val currentMonth = entries.filter { PersianDate.parts(it.occurredAt).key == currentParts.key }
    val income = currentMonth.filter { it.type == EntryType.INCOME }.sumOf { it.amount }
    val expense = currentMonth.filter { it.type == EntryType.EXPENSE }.sumOf { it.amount }
    val net = income - expense

    val months = PersianDate.lastMonths(2)
    val previousKey = months.firstOrNull()?.key
    val previousExpense = entries
        .filter { it.type == EntryType.EXPENSE && PersianDate.parts(it.occurredAt).key == previousKey }
        .sumOf { it.amount }

    val expenseDeltaText = when {
        previousExpense <= 0L -> "برای ماه قبل داده کافی نداریم."
        expense == previousExpense -> "هزینه این ماه با ماه قبل برابر است."
        else -> {
            val pct = (((expense - previousExpense).toDouble() / previousExpense) * 100.0).roundToInt()
            if (pct > 0) "هزینه این ماه ${pct.toString().toPersianDigits()}٪ بیشتر از ماه قبل است."
            else "هزینه این ماه ${(-pct).toString().toPersianDigits()}٪ کمتر از ماه قبل است."
        }
    }

    val topCategories = currentMonth
        .filter { it.type == EntryType.EXPENSE }
        .groupBy { it.category }
        .mapValues { (_, rows) -> rows.sumOf { it.amount } }
        .entries
        .sortedByDescending { it.value }
        .take(5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            PersianDate.formatMonth(System.currentTimeMillis()),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(Modifier.weight(1f), "درآمد", income.asCompactToman(), IncomeSoft)
            MetricCard(Modifier.weight(1f), "هزینه", expense.asCompactToman(), ExpenseSoft)
            MetricCard(Modifier.weight(1f), "مانده", net.asCompactToman(), BalanceSoft)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                SectionTitle("خلاصه ماه")
                Text(expenseDeltaText, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                Text("مانده خالص: ${net.asToman()}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
            }
        }

        if (budget > 0L) {
            val progress = (expense.toFloat() / budget.toFloat()).coerceIn(0f, 1f)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    SectionTitle("بودجه ماهانه")
                    Text("بودجه: ${budget.asToman()}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    Text("مصرف‌شده: ${expense.asToman()}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text(
                        if (expense <= budget) "باقی‌مانده بودجه: ${(budget - expense).asToman()}"
                        else "عبور از بودجه: ${(expense - budget).asToman()}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }

        SectionTitle("بیشترین هزینه‌ها")
        if (topCategories.isEmpty()) {
            EmptyState("هنوز هزینه‌ای برای این ماه ثبت نشده.")
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    topCategories.forEachIndexed { index, row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(row.key, fontWeight = FontWeight.SemiBold)
                            Text(row.value.asToman(), fontWeight = FontWeight.SemiBold)
                        }
                        if (index < topCategories.lastIndex) HorizontalDivider()
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun MetricCard(modifier: Modifier, title: String, value: String, background: Color) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = background)) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, fontSize = 12.sp)
        }
    }
}

@Composable
private fun TransactionsScreen(
    repo: LedgerRepository,
    refreshToken: Int,
    onEdit: (LedgerEntry) -> Unit,
    onEditDebt: (Debt) -> Unit,
    onDeleted: () -> Unit
) {
    val entries = remember(refreshToken) { repo.entries() }
    val debts = remember(refreshToken) { repo.debts() }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("همه") }
    var pendingDelete by remember { mutableStateOf<LedgerEntry?>(null) }
    var pendingDebtDelete by remember { mutableStateOf<Debt?>(null) }

    val filtered = remember(entries, query, filter) {
        entries.filter { entry ->
            val typeOk = when (filter) {
                "هزینه" -> entry.type == EntryType.EXPENSE
                "درآمد" -> entry.type == EntryType.INCOME
                "بدهی" -> false
                else -> true
            }
            val needle = query.trim()
            val textOk = needle.isBlank() || listOf(
                entry.category, entry.subcategory, entry.note, entry.tags.joinToString(" ")
            ).any { it.contains(needle, ignoreCase = true) }
            typeOk && textOk
        }
    }

    val filteredDebts = remember(debts, query) {
        val needle = query.trim()
        debts.filter { debt ->
            needle.isBlank() || debt.name.contains(needle, true) || debt.note.contains(needle, true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
                label = { Text("جستجو در تراکنش‌ها یا بدهی‌ها") }
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("همه", "هزینه", "درآمد", "بدهی")) { item ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { filter = item },
                        label = { Text(item) }
                    )
                }
            }
        }

        when (filter) {
            "بدهی" -> {
                if (filteredDebts.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState("هنوز بدهی ثبت نشده است.")
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            DebtSummaryCard(filteredDebts.sumOf { it.currentAmount }, filteredDebts.size)
                        }
                        items(filteredDebts, key = { "debt-${it.id}" }) { debt ->
                            DebtCard(
                                debt = debt,
                                onEdit = { onEditDebt(debt) },
                                onDelete = { pendingDebtDelete = debt }
                            )
                        }
                    }
                }
            }
            else -> {
                if (filtered.isEmpty() && (filter != "همه" || filteredDebts.isEmpty())) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState("موردی با این فیلتر پیدا نشد.")
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (filter == "همه" && debts.isNotEmpty()) {
                            item { DebtSummaryCard(debts.sumOf { it.currentAmount }, debts.size) }
                        }
                        items(filtered, key = { "entry-${it.id}" }) { entry ->
                            EntryCard(
                                entry = entry,
                                onEdit = { onEdit(entry) },
                                onDelete = { pendingDelete = entry }
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("حذف تراکنش") },
            text = { Text("این تراکنش حذف شود؟") },
            confirmButton = {
                TextButton(onClick = {
                    repo.delete(entry.id)
                    pendingDelete = null
                    onDeleted()
                }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("انصراف") } }
        )
    }

    pendingDebtDelete?.let { debt ->
        AlertDialog(
            onDismissRequest = { pendingDebtDelete = null },
            title = { Text("حذف بدهی") },
            text = { Text("بدهی «${debt.name}» و تاریخچه آن حذف شود؟") },
            confirmButton = {
                TextButton(onClick = {
                    repo.deleteDebt(debt.id)
                    pendingDebtDelete = null
                    onDeleted()
                }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { pendingDebtDelete = null }) { Text("انصراف") } }
        )
    }
}

@Composable
private fun DebtSummaryCard(total: Long, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DebtSoft)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text("بدهی‌های باز", fontWeight = FontWeight.Bold)
                Text("${count.toString().toPersianDigits()} مورد", fontSize = 12.sp)
            }
            Text(total.asToman(), color = DebtStrong, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EntryCard(entry: LedgerEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cardColor = if (entry.type == EntryType.INCOME) IncomeSoft else ExpenseSoft
    val amountColor = if (entry.type == EntryType.INCOME) IncomeStrong else ExpenseStrong
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(entry.category, fontWeight = FontWeight.Bold)
                    if (entry.subcategory.isNotBlank()) Text(entry.subcategory, fontSize = 12.sp)
                }
                Text(
                    (if (entry.type == EntryType.INCOME) "+" else "−") + " " + entry.amount.asToman(),
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }
            Text(PersianDate.format(entry.occurredAt), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, fontSize = 12.sp)
            if (entry.tags.isNotEmpty()) {
                Text(entry.tags.joinToString("  ") { "#$it" }, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, fontSize = 12.sp)
            }
            if (entry.note.isNotBlank()) {
                Text(entry.note, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, fontSize = 12.sp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                TextButton(onClick = onEdit) { Text("ویرایش") }
                TextButton(onClick = onDelete) { Text("حذف") }
            }
        }
    }
}

@Composable
private fun DebtCard(debt: Debt, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DebtSoft)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(debt.name, fontWeight = FontWeight.Bold)
                Text(debt.currentAmount.asToman(), color = DebtStrong, fontWeight = FontWeight.Bold)
            }
            if (debt.originalAmount != debt.currentAmount) {
                Text("مبلغ اولیه: ${debt.originalAmount.asToman()}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, fontSize = 12.sp)
            }
            Text("آخرین وضعیت: ${PersianDate.format(debt.occurredAt)}", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, fontSize = 12.sp)
            if (debt.note.isNotBlank()) {
                Text(debt.note, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, fontSize = 12.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                TextButton(onClick = onEdit) { Text("به‌روزرسانی مانده") }
                TextButton(onClick = onDelete) { Text("حذف") }
            }
        }
    }
}

@Composable
private fun AddEntryScreen(
    repo: LedgerRepository,
    refreshToken: Int,
    editing: LedgerEntry?,
    editingDebt: Debt?,
    onSaved: () -> Unit,
    onCancelEdit: () -> Unit
) {
    val initialKind = when {
        editingDebt != null -> "بدهی"
        editing?.type == EntryType.INCOME -> "درآمد"
        else -> "هزینه"
    }
    var kind by remember(editing?.id, editingDebt?.id) { mutableStateOf(initialKind) }
    var amountText by remember(editing?.id, editingDebt?.id) {
        mutableStateOf(
            (editing?.amount ?: editingDebt?.currentAmount)
                ?.toString()?.toPersianDigits().orEmpty()
        )
    }
    var category by remember(editing?.id) { mutableStateOf(editing?.category.orEmpty()) }
    var subcategory by remember(editing?.id) { mutableStateOf(editing?.subcategory.orEmpty()) }
    var customSourceName by remember(editing?.id) {
        mutableStateOf(
            if (editing?.type == EntryType.INCOME && editing.category in Presets.incomeNameableCategories) {
                editing.subcategory
            } else ""
        )
    }
    var debtName by remember(editingDebt?.id) { mutableStateOf(editingDebt?.name.orEmpty()) }
    var tags by remember(editing?.id) { mutableStateOf(editing?.tags?.toSet() ?: emptySet()) }
    var tagsExpanded by remember(editing?.id) { mutableStateOf(false) }
    var note by remember(editing?.id, editingDebt?.id) {
        mutableStateOf(editing?.note ?: editingDebt?.note.orEmpty())
    }
    val initialDate = editing?.occurredAt ?: editingDebt?.occurredAt ?: System.currentTimeMillis()
    var dateText by remember(editing?.id, editingDebt?.id) { mutableStateOf(PersianDate.format(initialDate)) }
    var error by remember(editing?.id, editingDebt?.id) { mutableStateOf<String?>(null) }

    val entryType = if (kind == "درآمد") EntryType.INCOME else EntryType.EXPENSE
    val customCategories = remember(refreshToken, entryType) { repo.customCategories(entryType) }
    val categoryMap = remember(customCategories, entryType) { Presets.mergedCategories(entryType, customCategories) }
    val allTags = remember(refreshToken) { (Presets.defaultTags + repo.customTags()).distinct() }
    val monthOptions = remember { PersianDate.lastMonths(12).reversed() }
    val dateParts = PersianDate.parse(dateText)?.let { PersianDate.parts(it) }
    val selectedMonthKey = dateParts?.key

    LaunchedEffect(kind, categoryMap) {
        if (kind != "بدهی") {
            if (category !in categoryMap.keys) {
                category = categoryMap.keys.firstOrNull().orEmpty()
                subcategory = categoryMap[category]?.firstOrNull().orEmpty()
                customSourceName = ""
            } else if (subcategory.isBlank() && category !in Presets.incomeNameableCategories) {
                subcategory = categoryMap[category]?.firstOrNull().orEmpty()
            }
        }
    }

    val subOptions = categoryMap[category].orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End
    ) {
        SectionTitle("نوع ثبت")
        val kindOptions = when {
            editingDebt != null -> listOf("بدهی")
            editing != null -> listOf("هزینه", "درآمد")
            else -> listOf("هزینه", "درآمد", "بدهی")
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(kindOptions) { item ->
                FilterChip(
                    selected = kind == item,
                    onClick = {
                        kind = item
                        if (item != initialKind) {
                            category = ""
                            subcategory = ""
                            customSourceName = ""
                        }
                    },
                    label = { Text(item) }
                )
            }
        }

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text(if (kind == "بدهی") "مانده فعلی بدهی به تومان" else "مبلغ به تومان") },
            supportingText = { amountText.toLongAmountOrNull()?.let { Text(it.asToman()) } }
        )

        if (kind == "بدهی") {
            OutlinedTextField(
                value = debtName,
                onValueChange = { debtName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
                label = { Text("نام بدهی / طلبکار") }
            )
            if (editingDebt != null) {
                Text(
                    "مبلغ اولیه: ${editingDebt.originalAmount.asToman()}",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    fontSize = 12.sp
                )
                Text(
                    "برای ثبت کاهش یا افزایش بدهی، مانده فعلی و تاریخ را تغییر دهید.",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    fontSize = 12.sp
                )
            }
        } else {
            SelectorField(
                label = "دسته",
                value = category.ifBlank { "انتخاب کنید" },
                options = categoryMap.keys.toList(),
                onSelect = {
                    category = it
                    subcategory = categoryMap[it]?.firstOrNull().orEmpty()
                    customSourceName = ""
                }
            )

            if (entryType == EntryType.INCOME && category in Presets.incomeNameableCategories) {
                OutlinedTextField(
                    value = customSourceName,
                    onValueChange = { customSourceName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
                    label = {
                        Text(
                            when (category) {
                                "شغل دوم" -> "اسم شغل دوم"
                                "شغل سوم" -> "اسم شغل سوم"
                                "قرض" -> "نام شخص / منبع قرض"
                                else -> "نام منبع درآمد"
                            }
                        )
                    }
                )
            } else if (subOptions.isNotEmpty()) {
                SelectorField(
                    label = "زیرمجموعه",
                    value = subcategory.ifBlank { "بدون زیرمجموعه" },
                    options = subOptions,
                    onSelect = { subcategory = it }
                )
            }
        }

        SectionTitle("ماه ثبت")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(monthOptions, key = { it.key }) { month ->
                FilterChip(
                    selected = selectedMonthKey == month.key,
                    onClick = {
                        val preferredDay = dateParts?.day ?: PersianDate.parts(System.currentTimeMillis()).day
                        dateText = PersianDate.format(PersianDate.dateForMonth(month, preferredDay))
                    },
                    label = { Text(PersianDate.monthLabel(month)) }
                )
            }
        }

        OutlinedTextField(
            value = dateText,
            onValueChange = { dateText = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("تاریخ شمسی") },
            supportingText = { Text("مثال: ۱۴۰۵/۰۵/۲۳") }
        )

        if (kind != "بدهی") {
            OutlinedButton(
                onClick = { tagsExpanded = !tagsExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (tags.isEmpty()) "تگ‌ها (اختیاری)"
                    else "تگ‌ها (${tags.size.toString().toPersianDigits()} انتخاب شده)"
                )
            }
            if (tagsExpanded) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allTags, key = { it }) { tag ->
                        FilterChip(
                            selected = tag in tags,
                            onClick = { tags = if (tag in tags) tags - tag else tags + tag },
                            label = { Text("#$tag") }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier.fillMaxWidth(),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
            label = { Text("توضیح (اختیاری)") },
            minLines = 2
        )

        error?.let {
            Text(
                it,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val amount = amountText.toLongAmountOrNull()
                val date = PersianDate.parse(dateText)
                if (kind == "بدهی") {
                    error = when {
                        amount == null || amount < 0L -> "مبلغ معتبر وارد کنید."
                        debtName.isBlank() -> "نام بدهی را وارد کنید."
                        date == null -> "تاریخ شمسی معتبر وارد کنید."
                        else -> null
                    }
                    if (error == null) {
                        repo.saveDebt(
                            Debt(
                                id = editingDebt?.id ?: 0L,
                                name = debtName.trim(),
                                originalAmount = editingDebt?.originalAmount ?: amount!!,
                                currentAmount = amount!!,
                                note = note.trim(),
                                occurredAt = date!!,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        onSaved()
                    }
                } else {
                    val effectiveSubcategory = if (
                        entryType == EntryType.INCOME && category in Presets.incomeNameableCategories
                    ) customSourceName.trim() else subcategory.trim()

                    error = when {
                        amount == null || amount <= 0L -> "مبلغ معتبر وارد کنید."
                        category.isBlank() -> "دسته را انتخاب کنید."
                        date == null -> "تاریخ شمسی معتبر وارد کنید."
                        entryType == EntryType.INCOME &&
                            category in setOf("شغل دوم", "شغل سوم", "سایر منابع") &&
                            effectiveSubcategory.isBlank() -> "برای این منبع درآمد یک نام وارد کنید."
                        else -> null
                    }
                    if (error == null) {
                        repo.save(
                            LedgerEntry(
                                id = editing?.id ?: 0L,
                                type = entryType,
                                amount = amount!!,
                                category = category,
                                subcategory = effectiveSubcategory,
                                tags = tags.toList().sorted(),
                                note = note.trim(),
                                occurredAt = date!!,
                                createdAt = editing?.createdAt ?: System.currentTimeMillis()
                            )
                        )
                        onSaved()
                    }
                }
            }
        ) {
            Text(
                when {
                    editingDebt != null -> "ثبت مانده جدید بدهی"
                    editing != null -> "ذخیره تغییرات"
                    kind == "بدهی" -> "ثبت بدهی"
                    else -> "ثبت تراکنش"
                }
            )
        }

        if (editing != null || editingDebt != null) {
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onCancelEdit) {
                Text("انصراف از ویرایش")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SelectorField(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label)
                Text(value, fontWeight = FontWeight.SemiBold)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

private fun monthSums(entries: List<LedgerEntry>, monthKey: String): Pair<Long, Long> {
    val rows = entries.filter { PersianDate.parts(it.occurredAt).key == monthKey }
    val income = rows.filter { it.type == EntryType.INCOME }.sumOf { it.amount }
    val expense = rows.filter { it.type == EntryType.EXPENSE }.sumOf { it.amount }
    return income to expense
}

private fun metricBars(metric: String, income: Long, expense: Long): List<ChartBar> = when (metric) {
    "درآمد" -> listOf(ChartBar("درآمد", income))
    "هزینه" -> listOf(ChartBar("هزینه", expense))
    else -> listOf(ChartBar("درآمد", income), ChartBar("هزینه", expense))
}

@Composable
private fun ComparisonScreen(repo: LedgerRepository, refreshToken: Int) {
    val entries = remember(refreshToken) { repo.entries() }
    val paletteId = remember(refreshToken) { repo.setting("chart_palette", "green_red") }
    val current = remember { PersianDate.lastMonths(1).first() }
    val last13 = remember { PersianDate.lastMonths(13) }
    val previousMonths = last13.dropLast(1).reversed()

    val currentSums = monthSums(entries, current.key)
    var selectedCompareKey by remember { mutableStateOf(previousMonths.firstOrNull()?.key.orEmpty()) }
    var monthlyMetric by remember { mutableStateOf("هر دو") }
    var benchmark by remember { mutableStateOf("میانگین ۳ ماه") }
    var benchmarkMetric by remember { mutableStateOf("هر دو") }

    val selectedCompareMonth = previousMonths.firstOrNull { it.key == selectedCompareKey }
        ?: previousMonths.firstOrNull()
    val selectedSums = selectedCompareMonth?.let { monthSums(entries, it.key) } ?: (0L to 0L)

    val avgMonths = when (benchmark) {
        "میانگین ۶ ماه" -> last13.dropLast(1).takeLast(6)
        "سال گذشته" -> emptyList()
        else -> last13.dropLast(1).takeLast(3)
    }
    val benchmarkSums = if (benchmark == "سال گذشته") {
        monthSums(entries, last13.first().key)
    } else {
        val pairs = avgMonths.map { monthSums(entries, it.key) }
        if (pairs.isEmpty()) 0L to 0L
        else pairs.sumOf { it.first } / pairs.size to pairs.sumOf { it.second } / pairs.size
    }

    val modes = listOf("دسته‌ها", "زیرمجموعه‌ها", "تگ‌ها", "روند یک آیتم", "بدهی‌ها")
    var mode by remember { mutableStateOf(modes.first()) }
    var monthCount by remember { mutableIntStateOf(6) }
    var selectedType by remember { mutableStateOf(EntryType.EXPENSE) }
    var criterion by remember { mutableStateOf("دسته") }
    var selectedItem by remember { mutableStateOf("") }

    val months = remember(monthCount) { PersianDate.lastMonths(monthCount) }
    val monthKeys = months.map { it.key }.toSet()
    val inRange = entries.filter { PersianDate.parts(it.occurredAt).key in monthKeys }

    val groups: List<ChartGroup>
    val helperText: String

    when (mode) {
        "دسته‌ها" -> {
            val rows = inRange.filter { it.type == selectedType }
            groups = rows.groupBy { it.category }
                .map { (name, list) -> ChartGroup(name, listOf(ChartBar(name, list.sumOf { it.amount }))) }
                .sortedByDescending { it.bars.first().value }
            helperText = "جمع دسته‌ها در ${monthCount.toString().toPersianDigits()} ماه اخیر."
        }
        "زیرمجموعه‌ها" -> {
            val rows = inRange.filter { it.type == selectedType && it.subcategory.isNotBlank() }
            groups = rows.groupBy { it.subcategory }
                .map { (name, list) -> ChartGroup(name, listOf(ChartBar(name, list.sumOf { it.amount }))) }
                .sortedByDescending { it.bars.first().value }
            helperText = "برای مثال بنزین، تاکسی، اجاره یا نام شغل‌ها را با هم مقایسه کنید."
        }
        "تگ‌ها" -> {
            val rows = inRange.filter { it.type == selectedType }
            val pairs = rows.flatMap { entry -> entry.tags.map { it to entry.amount } }
            groups = pairs.groupBy { it.first }
                .map { (name, list) -> ChartGroup(name, listOf(ChartBar(name, list.sumOf { it.second }))) }
                .sortedByDescending { it.bars.first().value }
            helperText = "مقایسه بر اساس تگ‌هایی مثل کاری، سفر، خانه و شخصی."
        }
        "بدهی‌ها" -> {
            groups = months.map { month ->
                ChartGroup(
                    label = month.label,
                    bars = listOf(ChartBar("بدهی", repo.debtTotalAt(PersianDate.endOfMonth(month))))
                )
            }
            helperText = "مانده کل بدهی‌ها در پایان هر ماه؛ کاهش یا افزایش بدهی را نشان می‌دهد."
        }
        else -> {
            val candidates = remember(inRange, selectedType, criterion) {
                val rows = inRange.filter { it.type == selectedType }
                when (criterion) {
                    "زیرمجموعه" -> rows.map { it.subcategory }.filter { it.isNotBlank() }
                    "تگ" -> rows.flatMap { it.tags }
                    else -> rows.map { it.category }
                }.distinct().sorted()
            }
            LaunchedEffect(candidates, criterion, selectedType) {
                if (selectedItem !in candidates) selectedItem = candidates.firstOrNull().orEmpty()
            }
            groups = months.map { month ->
                val rows = inRange.filter { entry ->
                    entry.type == selectedType &&
                        PersianDate.parts(entry.occurredAt).key == month.key &&
                        when (criterion) {
                            "زیرمجموعه" -> entry.subcategory == selectedItem
                            "تگ" -> selectedItem in entry.tags
                            else -> entry.category == selectedItem
                        }
                }
                ChartGroup(
                    label = month.label,
                    bars = listOf(ChartBar(selectedItem.ifBlank { "آیتم" }, rows.sumOf { it.amount }))
                )
            }
            helperText = if (selectedItem.isBlank()) "برای این معیار هنوز داده‌ای وجود ندارد."
            else "روند «$selectedItem» را ماه‌به‌ماه می‌بینید."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.End
    ) {
        SectionTitle("این ماه: درآمد در برابر هزینه")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ColumnBarChart(
                    groups = listOf(
                        ChartGroup(
                            PersianDate.monthLabel(current),
                            listOf(
                                ChartBar("درآمد", currentSums.first),
                                ChartBar("هزینه", currentSums.second)
                            )
                        )
                    ),
                    paletteId = paletteId,
                    chartHeight = 170
                )
                val difference = currentSums.first - currentSums.second
                Text(
                    "اختلاف درآمد و هزینه: ${difference.asToman()}",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        SectionTitle("مقایسه ماه جاری با یک ماه دیگر")
        if (previousMonths.isNotEmpty()) {
            SelectorField(
                label = "ماه مورد مقایسه",
                value = selectedCompareMonth?.let { PersianDate.monthLabel(it) }.orEmpty(),
                options = previousMonths.map { PersianDate.monthLabel(it) },
                onSelect = { label ->
                    previousMonths.firstOrNull { PersianDate.monthLabel(it) == label }?.let {
                        selectedCompareKey = it.key
                    }
                }
            )
            MetricChoice(monthlyMetric) { monthlyMetric = it }
            Card(modifier = Modifier.fillMaxWidth()) {
                ColumnBarChart(
                    groups = listOf(
                        ChartGroup(
                            selectedCompareMonth?.let { PersianDate.monthLabel(it) } ?: "ماه قبل",
                            metricBars(monthlyMetric, selectedSums.first, selectedSums.second)
                        ),
                        ChartGroup(
                            "ماه جاری",
                            metricBars(monthlyMetric, currentSums.first, currentSums.second)
                        )
                    ),
                    paletteId = paletteId,
                    chartHeight = 170
                )
            }
        }

        SectionTitle("مقایسه با میانگین یا سال گذشته")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("میانگین ۳ ماه", "میانگین ۶ ماه", "سال گذشته")) { item ->
                FilterChip(selected = benchmark == item, onClick = { benchmark = item }, label = { Text(item) })
            }
        }
        MetricChoice(benchmarkMetric) { benchmarkMetric = it }
        Card(modifier = Modifier.fillMaxWidth()) {
            ColumnBarChart(
                groups = listOf(
                    ChartGroup(
                        benchmark,
                        metricBars(benchmarkMetric, benchmarkSums.first, benchmarkSums.second)
                    ),
                    ChartGroup(
                        "ماه جاری",
                        metricBars(benchmarkMetric, currentSums.first, currentSums.second)
                    )
                ),
                paletteId = paletteId,
                chartHeight = 170
            )
        }

        HorizontalDivider()
        SectionTitle("تحلیل جزئی")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(modes) { item ->
                FilterChip(selected = mode == item, onClick = { mode = item }, label = { Text(item) })
            }
        }

        SectionTitle("بازه زمانی")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(3, 6, 12).forEach { count ->
                FilterChip(
                    selected = monthCount == count,
                    onClick = { monthCount = count },
                    label = { Text("${count.toString().toPersianDigits()} ماه") }
                )
            }
        }

        if (mode !in listOf("بدهی‌ها")) {
            SectionTitle("نوع تراکنش")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedType == EntryType.EXPENSE,
                    onClick = { selectedType = EntryType.EXPENSE; selectedItem = "" },
                    label = { Text("هزینه") }
                )
                FilterChip(
                    selected = selectedType == EntryType.INCOME,
                    onClick = { selectedType = EntryType.INCOME; selectedItem = "" },
                    label = { Text("درآمد") }
                )
            }
        }

        if (mode == "روند یک آیتم") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("دسته", "زیرمجموعه", "تگ").forEach { item ->
                    FilterChip(
                        selected = criterion == item,
                        onClick = { criterion = item; selectedItem = "" },
                        label = { Text(item) }
                    )
                }
            }
            val rows = inRange.filter { it.type == selectedType }
            val candidates = when (criterion) {
                "زیرمجموعه" -> rows.map { it.subcategory }.filter { it.isNotBlank() }
                "تگ" -> rows.flatMap { it.tags }
                else -> rows.map { it.category }
            }.distinct().sorted()
            if (candidates.isNotEmpty()) {
                SelectorField(
                    label = "آیتم مورد مقایسه",
                    value = selectedItem.ifBlank { candidates.first() },
                    options = candidates,
                    onSelect = { selectedItem = it }
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(helperText, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                if (groups.isEmpty() || groups.all { group -> group.bars.all { it.value == 0L } }) {
                    EmptyState("برای این مقایسه هنوز داده کافی ثبت نشده.")
                } else {
                    ColumnBarChart(groups = groups, paletteId = paletteId)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MetricChoice(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("هر دو", "درآمد", "هزینه").forEach { item ->
            FilterChip(selected = selected == item, onClick = { onSelect(item) }, label = { Text(item) })
        }
    }
}

@Composable
private fun ColumnBarChart(
    groups: List<ChartGroup>,
    paletteId: String,
    chartHeight: Int = 210
) {
    val maxValue = groups.flatMap { it.bars }.maxOfOrNull { it.value }?.coerceAtLeast(1L) ?: 1L
    val seriesLabels = groups.flatMap { it.bars.map { bar -> bar.label } }.distinct().take(4)

    if (seriesLabels.size > 1) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            seriesLabels.forEachIndexed { index, label ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(seriesColor(label, index, paletteId))
                    )
                    Text(label, fontSize = 12.sp)
                }
            }
        }
    }

    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll)
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        groups.forEach { group ->
            Column(
                modifier = Modifier.widthIn(min = if (group.bars.size > 1) 110.dp else 78.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.height(chartHeight.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    group.bars.forEachIndexed { index, bar ->
                        val ratio = bar.value.toFloat() / maxValue.toFloat()
                        val maxBar = (chartHeight - 52).coerceAtLeast(80)
                        val barHeight = if (bar.value == 0L) 3.dp else (maxBar * ratio).coerceAtLeast(10f).dp
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Text(
                                bar.value.asCompactToman(),
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(58.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(if (group.bars.size > 1) 30.dp else 38.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                                    .background(seriesColor(bar.label, index, paletteId))
                            )
                        }
                    }
                }
                Text(
                    group.label,
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    modifier = Modifier.widthIn(min = 72.dp, max = 120.dp)
                )
            }
        }
    }
}

private fun seriesColor(label: String, index: Int, paletteId: String): Color {
    val palette = chartPalettes.firstOrNull { it.id == paletteId } ?: chartPalettes.first()
    return when {
        label.contains("درآمد") -> palette.income
        label.contains("هزینه") -> palette.expense
        label.contains("بدهی") -> DebtStrong
        index % 3 == 0 -> palette.income
        index % 3 == 1 -> palette.expense
        else -> palette.other
    }
}

@Composable
private fun SettingsScreen(
    repo: LedgerRepository,
    refreshToken: Int,
    onChanged: () -> Unit
) {
    var budgetText by remember(refreshToken) {
        mutableStateOf(
            repo.budget().takeIf { it > 0L }?.toString()?.toPersianDigits().orEmpty()
        )
    }
    var categoryType by remember { mutableStateOf(EntryType.EXPENSE) }
    var categoryName by remember { mutableStateOf("") }
    var subcategoryName by remember { mutableStateOf("") }
    var tagName by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    val selectedTheme = remember(refreshToken) { repo.setting("visual_theme", "lavender") }
    val selectedPalette = remember(refreshToken) { repo.setting("chart_palette", "green_red") }
    val customCategories = remember(refreshToken) { repo.customCategories() }
    val customTags = remember(refreshToken) { repo.customTags() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.End
    ) {
        SectionTitle("ظاهر برنامه")
        Text("پس‌زمینه", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(visualThemes) { theme ->
                FilterChip(
                    selected = selectedTheme == theme.id,
                    onClick = {
                        repo.setSetting("visual_theme", theme.id)
                        onChanged()
                    },
                    label = { Text(theme.title) }
                )
            }
        }

        Text("رنگ نمودارها", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, fontWeight = FontWeight.SemiBold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(chartPalettes) { palette ->
                FilterChip(
                    selected = selectedPalette == palette.id,
                    onClick = {
                        repo.setSetting("chart_palette", palette.id)
                        onChanged()
                    },
                    label = { Text(palette.title) }
                )
            }
        }

        HorizontalDivider()
        SectionTitle("بودجه ماهانه")
        OutlinedTextField(
            value = budgetText,
            onValueChange = { budgetText = it },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
            label = { Text("بودجه به تومان") }
        )
        Button(onClick = {
            val amount = budgetText.toLongAmountOrNull() ?: 0L
            repo.setBudget(amount)
            status = "بودجه ذخیره شد."
            onChanged()
        }) { Text("ذخیره بودجه") }

        HorizontalDivider()
        SectionTitle("دسته و زیرمجموعه سفارشی")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = categoryType == EntryType.EXPENSE,
                onClick = { categoryType = EntryType.EXPENSE },
                label = { Text("هزینه") }
            )
            FilterChip(
                selected = categoryType == EntryType.INCOME,
                onClick = { categoryType = EntryType.INCOME },
                label = { Text("درآمد") }
            )
        }
        OutlinedTextField(
            value = categoryName,
            onValueChange = { categoryName = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
            label = { Text("نام دسته") }
        )
        OutlinedTextField(
            value = subcategoryName,
            onValueChange = { subcategoryName = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
            label = { Text("نام زیرمجموعه (اختیاری)") }
        )
        Button(onClick = {
            if (categoryName.isBlank()) {
                status = "نام دسته را وارد کنید."
            } else {
                repo.addCategory(categoryType, categoryName, subcategoryName)
                categoryName = ""
                subcategoryName = ""
                status = "دسته ذخیره شد."
                onChanged()
            }
        }) { Text("افزودن دسته") }

        if (customCategories.isNotEmpty()) {
            Text("دسته‌های ساخته‌شده:", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start, fontWeight = FontWeight.SemiBold)
            customCategories.forEach { row ->
                Text(
                    "• ${row.type.titleFa}: ${row.name}" + if (row.subcategory.isBlank()) "" else " ← ${row.subcategory}",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }
        }

        HorizontalDivider()
        SectionTitle("تگ سفارشی")
        OutlinedTextField(
            value = tagName,
            onValueChange = { tagName = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
            label = { Text("نام تگ") }
        )
        Button(onClick = {
            if (tagName.isBlank()) {
                status = "نام تگ را وارد کنید."
            } else {
                repo.addTag(tagName)
                tagName = ""
                status = "تگ ذخیره شد."
                onChanged()
            }
        }) { Text("افزودن تگ") }

        if (customTags.isNotEmpty()) {
            Text(
                customTags.joinToString("   ") { "#$it" },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        HorizontalDivider()
        SectionTitle("ورود خودکار از پیامک بانکی")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BalanceSoft)
        ) {
            Text(
                "ساختار برنامه برای اضافه‌شدن این قابلیت آماده است. در این نسخه مجوز خواندن پیامک فعال نشده تا نصب آزمایشی و Play Protect پیچیده‌تر نشود؛ آن را در مرحله بعد با صفحه بررسی و تأیید تراکنش‌های تشخیص‌داده‌شده اضافه می‌کنیم.",
                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        status?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(it, modifier = Modifier.padding(12.dp).fillMaxWidth(), textAlign = TextAlign.Start)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EmptyState(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            message,
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}
