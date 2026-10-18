package pv.domain.calendar.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pv.domain.calendar.dto.ConflictResponse;
import pv.domain.calendar.dto.EventCreateRequest;
import pv.domain.calendar.dto.EventResponse;
import pv.domain.calendar.dto.EventUpdateRequest;
import pv.domain.calendar.service.CalendarService;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@RestController
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class CalendarController {
    private static final String MEMBER_ID = "memberId";
    private final CalendarService calendarService;

    @GetMapping
    public List<EventResponse> getCalendar(@AuthenticationPrincipal Long memberId,
                                           @RequestParam @DateTimeFormat(iso = DATE) LocalDate from,
                                           @RequestParam @DateTimeFormat(iso = DATE) LocalDate to,
                                           @RequestParam(required = false) Set<Long> boardIds,
                                           @RequestParam(defaultValue = "false") boolean mine) {
        return calendarService.getCalendar(memberId, from, to, boardIds, mine);
    }

    @GetMapping("/conflicts")
    public List<ConflictResponse> getConflicts(@AuthenticationPrincipal Long memberId,
                                               @RequestParam @DateTimeFormat(iso = DATE) LocalDate from,
                                               @RequestParam @DateTimeFormat(iso = DATE) LocalDate to) {
        return calendarService.getConflicts(memberId, from, to);
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@AuthenticationPrincipal Long memberId,
                                @RequestBody @Valid EventCreateRequest req) {
        return calendarService.create(memberId, req);
    }

    @PatchMapping("/events/{eventId}")
    public EventResponse update(@AuthenticationPrincipal Long memberId,
                                @PathVariable Long eventId,
                                @RequestBody @Valid EventUpdateRequest req) {
        return calendarService.update(memberId, eventId, req);
    }

    @DeleteMapping("/events/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long memberId,
                       @PathVariable Long eventId) {
        calendarService.delete(memberId, eventId);
    }
}
