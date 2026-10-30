package com.example.registration.waitlist.adapter.in.web;

import com.example.registration.waitlist.application.EnrollmentService;
import com.example.registration.waitlist.domain.CourseCode;
import com.example.registration.waitlist.domain.DropResult;
import com.example.registration.waitlist.domain.EnrollResult;
import com.example.registration.waitlist.domain.Identifiers;
import com.example.registration.waitlist.domain.LearnerId;
import com.example.registration.waitlist.domain.OfferingId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP boundary for one offering's enrollments. Maps each domain outcome to the status code in DESIGN.md
 * section 3. Validation failures and unknown references are handled by {@link GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/offerings/{offeringId}")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = Objects.requireNonNull(enrollmentService, "enrollmentService");
    }

    @PostMapping("/enrollments")
    public ResponseEntity<Object> enroll(
            @PathVariable @Pattern(regexp = Identifiers.PATTERN_TEXT) String offeringId,
            @Valid @RequestBody EnrollmentRequest request) {
        EnrollResult result = enrollmentService.enroll(new OfferingId(offeringId), new LearnerId(request.learnerId()));
        return toResponse(result);
    }

    @DeleteMapping("/enrollments/{learnerId}")
    public ResponseEntity<EnrollmentResponse> drop(
            @PathVariable @Pattern(regexp = Identifiers.PATTERN_TEXT) String offeringId,
            @PathVariable @Pattern(regexp = Identifiers.PATTERN_TEXT) String learnerId) {
        DropResult result = enrollmentService.drop(new OfferingId(offeringId), new LearnerId(learnerId));
        return ResponseEntity.ok(toBody(result));
    }

    @GetMapping
    public OfferingResponse view(@PathVariable @Pattern(regexp = Identifiers.PATTERN_TEXT) String offeringId) {
        return OfferingResponse.from(enrollmentService.view(new OfferingId(offeringId)));
    }

    private static ResponseEntity<Object> toResponse(EnrollResult result) {
        return switch (result) {
            case EnrollResult.Enrolled enrolled ->
                    ResponseEntity.status(HttpStatus.CREATED).body(EnrollmentResponse.from(enrolled));
            case EnrollResult.Waitlisted waitlisted ->
                    ResponseEntity.status(HttpStatus.ACCEPTED).body(EnrollmentResponse.waiting(
                            Outcome.WAITLISTED, waitlisted.learnerId(), waitlisted.position()));
            case EnrollResult.AlreadyEnrolled already ->
                    ResponseEntity.ok(EnrollmentResponse.of(Outcome.ALREADY_ENROLLED, already.learnerId()));
            case EnrollResult.AlreadyWaitlisted already ->
                    ResponseEntity.ok(EnrollmentResponse.waiting(
                            Outcome.ALREADY_WAITLISTED, already.learnerId(), already.position()));
            case EnrollResult.PrerequisitesNotMet notMet ->
                    ResponseEntity.unprocessableEntity().body(prerequisiteProblem(notMet));
        };
    }

    private static EnrollmentResponse toBody(DropResult result) {
        return switch (result) {
            case DropResult.Dropped dropped -> EnrollmentResponse.from(dropped);
            case DropResult.LeftWaitlist left -> EnrollmentResponse.of(Outcome.LEFT_WAITLIST, left.learnerId());
            case DropResult.NotEnrolled notEnrolled ->
                    EnrollmentResponse.of(Outcome.NOT_ENROLLED, notEnrolled.learnerId());
        };
    }

    private static ProblemDetail prerequisiteProblem(EnrollResult.PrerequisitesNotMet notMet) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY,
                "Learner " + notMet.learnerId() + " has not completed all prerequisites");
        problem.setTitle("Prerequisites not met");
        problem.setProperty(GlobalExceptionHandler.OUTCOME_PROPERTY, Outcome.PREREQUISITES_NOT_MET);
        problem.setProperty("missingPrerequisites",
                notMet.missingPrerequisites().stream().map(CourseCode::value).toList());
        return problem;
    }
}
