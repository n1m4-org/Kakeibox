package com.kakeibox.ui

import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val yenFormat = NumberFormat.getNumberInstance(Locale.JAPAN)
private val dayFormat = DateTimeFormatter.ofPattern("M月d日(E)", Locale.JAPAN)
private val monthFormat = DateTimeFormatter.ofPattern("yyyy年M月", Locale.JAPAN)
private val fullDateFormat = DateTimeFormatter.ofPattern("yyyy年M月d日(E)", Locale.JAPAN)

fun yen(v: Long): String = "¥" + yenFormat.format(v)
fun signedYen(v: Long): String = (if (v < 0) "-" else "") + yen(kotlin.math.abs(v))
fun LocalDate.dayLabel(): String = format(dayFormat)
fun LocalDate.fullLabel(): String = format(fullDateFormat)
fun YearMonth.label(): String = format(monthFormat)
