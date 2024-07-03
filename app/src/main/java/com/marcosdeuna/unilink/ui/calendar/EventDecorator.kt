import android.graphics.Color
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.util.*

class EventDecorator(private val eventDates: List<CalendarDay>) : DayViewDecorator {

    private val dates: HashSet<CalendarDay> = HashSet(eventDates)

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return dates.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        view.addSpan(DotSpan(10f, Color.RED)) // Puedes cambiar el color y tamaño del punto aquí
    }
}
