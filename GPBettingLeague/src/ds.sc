import java.util.Locale

import org.joda.time.{DateTime, Days}

DateTime.now().toString("dd MMM yyyy (HH mm)",Locale.ENGLISH)

Days.daysBetween(DateTime.now, DateTime.now.plusDays(4)).getDays
Days.daysBetween(DateTime.now, DateTime.now.minusDays(4)).getDays