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
import androidx.compose.foundation.layout.weight
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LocalLayoutDirection
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KharjYarTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    LedgerApp()
                }
            }
        }
    }
}

@Composable
fun KharjYarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        typography = Typography(),
        content = content
    )
}

private data class BottomTab(
    val title: String,
    val icon: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerApp() {
    val context = LocalContext.current
    val repo = remember { LedgerRepository(context) }

    val tabs = listOf(
        BottomTab("خانه", "⌂"),
        BottomTab("تراکنش‌ها", "≡"),
        BottomTab("ثبت", "＋"),
        BottomTab("مقایسه", "▥"),
        BottomTab("تنظیمات", "⚙")
    )

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var refreshToken by remember { mutableIntStateOf(0) }
    var editingEntry by remember { mutableStateOf<LedgerEntry?>(null) }

    val title = if (selectedTab == 2 && editingEntry != null) {
        "ویرایش تراکنش"
    } else {
        when (selectedTab) {
            0 -> "خرج‌یار"
            1 -> "تراکنش‌ها"
            2 -> "ثبت تراکنش"
            3 -> "مقایسه هزینه و درآمد"
            else -> "تنظیمات"
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text(title) })
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            if (index != 2) editingEntry = null
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
                        selectedTab = 2
                    },
                    onDeleted = { refreshToken++ }
                )
                2 -> AddEntryScreen(
                    repo = repo,
                    refreshToken = refreshToken,
                    editing = editingEntry,
                    onSaved = {
                        refreshToken++
                        editingEntry = null
                        selectedTab = 1
                    },
                    onCancelEdit = {
                        editingEntry = null
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

@Composable
private fun DashboardScreen(
    repo: LedgerRepository,
    refreshToken: Int
) {
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
            if (pct > 0) {
                "هزینه این ماه ${pct.toString().toPersianDigits()}٪ بیشتر از ماه قبل است."
            } else {
                "هزینه این ماه ${(-pct).toString().toPersianDigits()}٪ کمتر از ماه قبل است."
            }
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            PersianDate.formatMonth(System.currentTimeMillis()),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "درآمد",
                value = income.asCompactToman()
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "هزینه",
                value = expense.asCompactToman()
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                title = "مانده",
                value = net.asCompactToman()
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("خلاصه ماه", fontWeight = FontWeight.Bold)
                Text(expenseDeltaText)
                Text("مانده خالص: ${net.asToman()}")
            }
        }

        if (budget > 0L) {
            val progress = (expense.toFloat() / budget.toFloat()).coerceIn(0f, 1f)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("بودجه ماهانه", fontWeight = FontWeight.Bold)
                    Text("بودجه: ${budget.asToman()}")
                    Text("مصرف‌شده: ${expense.asToman()}")
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        if (expense <= budget) {
                            "باقی‌مانده بودجه: ${(budget - expense).asToman()}"
                        } else {
                            "عبور از بودجه: ${(expense - budget).asToman()}"
                        }
                    )
                }
            }
        }

        Text("بیشترین هزینه‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (topCategories.isEmpty()) {
            EmptyState("هنوز هزینه‌ای برای این ماه ثبت نشده.")
        } else {
            topCategories.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(row.key)
                    Text(row.value.asToman(), fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider()
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun TransactionsScreen(
    repo: LedgerRepository,
    refreshToken: Int,
    onEdit: (LedgerEntry) -> Unit,
    onDeleted: () -> Unit
) {
    val entries = remember(refreshToken) { repo.entries() }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<String>("همه") }
    var pendingDelete by remember { mutableStateOf<LedgerEntry?>(null) }

    val filtered = remember(entries, query, filter) {
        entries.filter { entry ->
            val typeOk = when (filter) {
                "هزینه" -> entry.type == EntryType.EXPENSE
                "درآمد" -> entry.type == EntryType.INCOME
                else -> true
            }
            val needle = query.trim()
            val textOk = needle.isBlank() || listOf(
                entry.category,
                entry.subcategory,
                entry.note,
                entry.tags.joinToString(" ")
            ).any { it.contains(needle, ignoreCase = true) }
            typeOk && textOk
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("جستجو در هزینه، درآمد، تگ یا توضیح") }
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("همه", "هزینه", "درآمد")) { item ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { filter = item },
                        label = { Text(item) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EmptyState("تراکنشی با این فیلتر پیدا نشد.")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { entry ->
                    EntryCard(
                        entry = entry,
                        onEdit = { onEdit(entry) },
                        onDelete = { pendingDelete = entry }
                    )
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
                TextButton(
                    onClick = {
                        repo.delete(entry.id)
                        pendingDelete = null
                        onDeleted()
                    }
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
private fun EntryCard(
    entry: LedgerEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        entry.category,
                        fontWeight = FontWeight.Bold
                    )
                    if (entry.subcategory.isNotBlank()) {
                        Text(entry.subcategory, fontSize = 12.sp)
                    }
                }
                Text(
                    (if (entry.type == EntryType.INCOME) "+" else "−") + " " + entry.amount.asToman(),
                    fontWeight = FontWeight.Bold
                )
            }

            Text(PersianDate.format(entry.occurredAt), fontSize = 12.sp)

            if (entry.tags.isNotEmpty()) {
                Text(entry.tags.joinToString("  ") { "#$it" }, fontSize = 12.sp)
            }

            if (entry.note.isNotBlank()) {
                Text(entry.note, fontSize = 12.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text("ویرایش") }
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
    onSaved: () -> Unit,
    onCancelEdit: () -> Unit
) {
    var type by remember(editing?.id) {
        mutableStateOf(editing?.type ?: EntryType.EXPENSE)
    }
    var amountText by remember(editing?.id) {
        mutableStateOf(editing?.amount?.toString()?.toPersianDigits() ?: "")
    }
    var category by remember(editing?.id) {
        mutableStateOf(editing?.category ?: "")
    }
    var subcategory by remember(editing?.id) {
        mutableStateOf(editing?.subcategory ?: "")
    }
    var customSourceName by remember(editing?.id) {
        mutableStateOf(
            if (
                editing?.type == EntryType.INCOME &&
                editing.category in Presets.incomeNameableCategories
            ) editing.subcategory else ""
        )
    }
    var tags by remember(editing?.id) {
        mutableStateOf(editing?.tags?.toSet() ?: emptySet())
    }
    var note by remember(editing?.id) {
        mutableStateOf(editing?.note ?: "")
    }
    var dateText by remember(editing?.id) {
        mutableStateOf(PersianDate.format(editing?.occurredAt ?: System.currentTimeMillis()))
    }
    var error by remember(editing?.id) { mutableStateOf<String?>(null) }

    val customCategories = remember(refreshToken, type) {
        repo.customCategories(type)
    }
    val categoryMap = remember(customCategories, type) {
        Presets.mergedCategories(type, customCategories)
    }
    val allTags = remember(refreshToken) {
        (Presets.defaultTags + repo.customTags()).distinct()
    }

    LaunchedEffect(type, categoryMap) {
        if (category !in categoryMap.keys) {
            category = categoryMap.keys.firstOrNull().orEmpty()
            subcategory = categoryMap[category]?.firstOrNull().orEmpty()
            customSourceName = ""
        } else if (
            subcategory.isBlank() &&
            category !in Presets.incomeNameableCategories
        ) {
            subcategory = categoryMap[category]?.firstOrNull().orEmpty()
        }
    }

    val subOptions = categoryMap[category].orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("نوع تراکنش", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = type == EntryType.EXPENSE,
                onClick = {
                    type = EntryType.EXPENSE
                    category = ""
                    subcategory = ""
                    customSourceName = ""
                },
                label = { Text("هزینه") }
            )
            FilterChip(
                selected = type == EntryType.INCOME,
                onClick = {
                    type = EntryType.INCOME
                    category = ""
                    subcategory = ""
                    customSourceName = ""
                },
                label = { Text("درآمد") }
            )
        }

        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("مبلغ به تومان") },
            supportingText = {
                amountText.toLongAmountOrNull()?.let { Text(it.asToman()) }
            }
        )

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

        if (type == EntryType.INCOME && category in Presets.incomeNameableCategories) {
            OutlinedTextField(
                value = customSourceName,
                onValueChange = { customSourceName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
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

        OutlinedTextField(
            value = dateText,
            onValueChange = { dateText = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("تاریخ شمسی") },
            supportingText = { Text("مثال: ۱۴۰۵/۰۵/۲۳") }
        )

        Text("تگ‌ها", fontWeight = FontWeight.Bold)
        if (allTags.isEmpty()) {
            Text("از تنظیمات می‌توانید تگ بسازید.")
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allTags, key = { it }) { tag ->
                    FilterChip(
                        selected = tag in tags,
                        onClick = {
                            tags = if (tag in tags) tags - tag else tags + tag
                        },
                        label = { Text("#$tag") }
                    )
                }
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("توضیح (اختیاری)") },
            minLines = 2
        )

        error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val amount = amountText.toLongAmountOrNull()
                val date = PersianDate.parse(dateText)
                val effectiveSubcategory = if (
                    type == EntryType.INCOME &&
                    category in Presets.incomeNameableCategories
                ) {
                    customSourceName.trim()
                } else {
                    subcategory.trim()
                }

                error = when {
                    amount == null || amount <= 0L -> "مبلغ معتبر وارد کنید."
                    category.isBlank() -> "دسته را انتخاب کنید."
                    date == null -> "تاریخ شمسی معتبر وارد کنید."
                    type == EntryType.INCOME &&
                        category in setOf("شغل دوم", "شغل سوم", "سایر منابع") &&
                        effectiveSubcategory.isBlank() -> "برای این منبع درآمد یک نام وارد کنید."
                    else -> null
                }

                if (error == null) {
                    repo.save(
                        LedgerEntry(
                            id = editing?.id ?: 0L,
                            type = type,
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
        ) {
            Text(if (editing == null) "ثبت تراکنش" else "ذخیره تغییرات")
        }

        if (editing != null) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onCancelEdit
            ) {
                Text("انصراف از ویرایش")
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SelectorField(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label)
                Text(value, fontWeight = FontWeight.SemiBold)
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun ComparisonScreen(
    repo: LedgerRepository,
    refreshToken: Int
) {
    val entries = remember(refreshToken) { repo.entries() }

    val modes = listOf("درآمد ↔ هزینه", "دسته‌ها", "زیرمجموعه‌ها", "تگ‌ها", "روند یک آیتم")
    var mode by remember { mutableStateOf(modes.first()) }
    var monthCount by remember { mutableIntStateOf(6) }
    var selectedType by remember { mutableStateOf(EntryType.EXPENSE) }
    var criterion by remember { mutableStateOf("دسته") }
    var selectedItem by remember { mutableStateOf("") }

    val months = remember(monthCount) { PersianDate.lastMonths(monthCount) }
    val monthKeys = months.map { it.key }.toSet()
    val inRange = entries.filter { PersianDate.parts(it.occurredAt).key in monthKeys }

    val groups: List<ChartGroup>
    var helperText = ""

    when (mode) {
        "درآمد ↔ هزینه" -> {
            groups = months.map { month ->
                val rows = inRange.filter { PersianDate.parts(it.occurredAt).key == month.key }
                ChartGroup(
                    label = month.label,
                    bars = listOf(
                        ChartBar("درآمد", rows.filter { it.type == EntryType.INCOME }.sumOf { it.amount }),
                        ChartBar("هزینه", rows.filter { it.type == EntryType.EXPENSE }.sumOf { it.amount })
                    )
                )
            }
            helperText = "دو ستون کنار هم، جمع درآمد و هزینه هر ماه را نشان می‌دهد."
        }

        "دسته‌ها" -> {
            val rows = inRange.filter { it.type == selectedType }
            groups = rows
                .groupBy { it.category }
                .map { (name, list) -> ChartGroup(name, listOf(ChartBar(name, list.sumOf { it.amount }))) }
                .sortedByDescending { it.bars.first().value }
            helperText = "جمع دسته‌ها در ${monthCount.toString().toPersianDigits()} ماه اخیر."
        }

        "زیرمجموعه‌ها" -> {
            val rows = inRange
                .filter { it.type == selectedType && it.subcategory.isNotBlank() }
            groups = rows
                .groupBy { it.subcategory }
                .map { (name, list) -> ChartGroup(name, listOf(ChartBar(name, list.sumOf { it.amount }))) }
                .sortedByDescending { it.bars.first().value }
            helperText = "برای مثال می‌توانید بنزین، تاکسی، اجاره یا نام شغل‌ها را با هم مقایسه کنید."
        }

        "تگ‌ها" -> {
            val rows = inRange.filter { it.type == selectedType }
            val tagPairs = rows.flatMap { entry -> entry.tags.map { it to entry.amount } }
            groups = tagPairs
                .groupBy { it.first }
                .map { (name, list) -> ChartGroup(name, listOf(ChartBar(name, list.sumOf { it.second }))) }
                .sortedByDescending { it.bars.first().value }
            helperText = "مقایسه بر اساس تگ‌هایی مثل کاری، سفر، خانه و شخصی."
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
                if (selectedItem !in candidates) {
                    selectedItem = candidates.firstOrNull().orEmpty()
                }
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
            helperText = if (selectedItem.isBlank()) {
                "برای این معیار هنوز داده‌ای وجود ندارد."
            } else {
                "روند «$selectedItem» را ماه‌به‌ماه می‌بینید."
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("نوع مقایسه", fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(modes) { item ->
                FilterChip(
                    selected = mode == item,
                    onClick = { mode = item },
                    label = { Text(item) }
                )
            }
        }

        Text("بازه زمانی", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(3, 6, 12).forEach { count ->
                FilterChip(
                    selected = monthCount == count,
                    onClick = { monthCount = count },
                    label = { Text("${count.toString().toPersianDigits()} ماه") }
                )
            }
        }

        if (mode != "درآمد ↔ هزینه") {
            Text("نوع تراکنش", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedType == EntryType.EXPENSE,
                    onClick = {
                        selectedType = EntryType.EXPENSE
                        selectedItem = ""
                    },
                    label = { Text("هزینه") }
                )
                FilterChip(
                    selected = selectedType == EntryType.INCOME,
                    onClick = {
                        selectedType = EntryType.INCOME
                        selectedItem = ""
                    },
                    label = { Text("درآمد") }
                )
            }
        }

        if (mode == "روند یک آیتم") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("دسته", "زیرمجموعه", "تگ").forEach { item ->
                    FilterChip(
                        selected = criterion == item,
                        onClick = {
                            criterion = item
                            selectedItem = ""
                        },
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(helperText)
                if (groups.isEmpty() || groups.all { group -> group.bars.all { it.value == 0L } }) {
                    EmptyState("برای این مقایسه هنوز داده کافی ثبت نشده.")
                } else {
                    ColumnBarChart(groups = groups)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ColumnBarChart(
    groups: List<ChartGroup>
) {
    val maxValue = groups
        .flatMap { it.bars }
        .maxOfOrNull { it.value }
        ?.coerceAtLeast(1L)
        ?: 1L

    val seriesLabels = groups
        .flatMap { it.bars.map { bar -> bar.label } }
        .distinct()
        .take(3)

    if (seriesLabels.size > 1) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            seriesLabels.forEachIndexed { index, label ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(seriesColor(index))
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
                modifier = Modifier.widthIn(min = if (group.bars.size > 1) 100.dp else 72.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.height(190.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    group.bars.forEachIndexed { index, bar ->
                        val ratio = bar.value.toFloat() / maxValue.toFloat()
                        val barHeight = if (bar.value == 0L) 2.dp else (150f * ratio).coerceAtLeast(8f).dp

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Text(
                                bar.value.asCompactToman(),
                                fontSize = 9.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(52.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(if (group.bars.size > 1) 28.dp else 34.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(seriesColor(index))
                            )
                        }
                    }
                }

                Text(
                    group.label,
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    modifier = Modifier.widthIn(min = 68.dp, max = 110.dp)
                )
            }
        }
    }
}

@Composable
private fun seriesColor(index: Int) = when (index % 3) {
    0 -> MaterialTheme.colorScheme.primary
    1 -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.tertiary
}

@Composable
private fun SettingsScreen(
    repo: LedgerRepository,
    refreshToken: Int,
    onChanged: () -> Unit
) {
    var budgetText by remember(refreshToken) {
        mutableStateOf(
            repo.budget()
                .takeIf { it > 0L }
                ?.toString()
                ?.toPersianDigits()
                ?: ""
        )
    }
    var categoryType by remember { mutableStateOf(EntryType.EXPENSE) }
    var categoryName by remember { mutableStateOf("") }
    var subcategoryName by remember { mutableStateOf("") }
    var tagName by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }

    val customCategories = remember(refreshToken) { repo.customCategories() }
    val customTags = remember(refreshToken) { repo.customTags() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("بودجه ماهانه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = budgetText,
            onValueChange = { budgetText = it },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            label = { Text("بودجه به تومان") }
        )
        Button(
            onClick = {
                val amount = budgetText.toLongAmountOrNull() ?: 0L
                repo.setBudget(amount)
                status = "بودجه ذخیره شد."
                onChanged()
            }
        ) {
            Text("ذخیره بودجه")
        }

        HorizontalDivider()

        Text("دسته و زیرمجموعه سفارشی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
            label = { Text("نام دسته") }
        )
        OutlinedTextField(
            value = subcategoryName,
            onValueChange = { subcategoryName = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("نام زیرمجموعه (اختیاری)") }
        )
        Button(
            onClick = {
                if (categoryName.isBlank()) {
                    status = "نام دسته را وارد کنید."
                } else {
                    repo.addCategory(categoryType, categoryName, subcategoryName)
                    categoryName = ""
                    subcategoryName = ""
                    status = "دسته ذخیره شد."
                    onChanged()
                }
            }
        ) {
            Text("افزودن دسته")
        }

        if (customCategories.isNotEmpty()) {
            Text("دسته‌های ساخته‌شده:", fontWeight = FontWeight.SemiBold)
            customCategories.forEach { row ->
                Text(
                    "• ${row.type.titleFa}: ${row.name}" +
                        if (row.subcategory.isBlank()) "" else " ← ${row.subcategory}"
                )
            }
        }

        HorizontalDivider()

        Text("تگ سفارشی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = tagName,
            onValueChange = { tagName = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("نام تگ") }
        )
        Button(
            onClick = {
                if (tagName.isBlank()) {
                    status = "نام تگ را وارد کنید."
                } else {
                    repo.addTag(tagName)
                    tagName = ""
                    status = "تگ ذخیره شد."
                    onChanged()
                }
            }
        ) {
            Text("افزودن تگ")
        }

        if (customTags.isNotEmpty()) {
            Text(customTags.joinToString("   ") { "#$it" })
        }

        status?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(it, modifier = Modifier.padding(12.dp))
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
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}
