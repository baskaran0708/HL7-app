package com.livemedica.helix.data.mock

import com.livemedica.helix.core.utils.ClinicalFormat
import com.livemedica.helix.domain.model.Appointment
import com.livemedica.helix.domain.model.AppointmentStatus
import com.livemedica.helix.domain.model.Doctor
import com.livemedica.helix.domain.model.Facility
import com.livemedica.helix.domain.model.HelixNotification
import com.livemedica.helix.domain.model.HistoryEntry
import com.livemedica.helix.domain.model.Hl7AckStatus
import com.livemedica.helix.domain.model.Hl7Message
import com.livemedica.helix.domain.model.Hl7MessageType
import com.livemedica.helix.domain.model.InterfaceChannel
import com.livemedica.helix.domain.model.InterfaceState
import com.livemedica.helix.domain.model.Modality
import com.livemedica.helix.domain.model.NotificationCategory
import com.livemedica.helix.domain.model.NotificationTarget
import com.livemedica.helix.domain.model.Order
import com.livemedica.helix.domain.model.OrderStatus
import com.livemedica.helix.domain.model.Patient
import com.livemedica.helix.domain.model.Priority
import com.livemedica.helix.domain.model.RadiologyReport
import com.livemedica.helix.domain.model.ReportStatus
import com.livemedica.helix.domain.model.Sex
import com.livemedica.helix.domain.model.Study
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * The Phase-1 dataset.
 *
 * Content is written to be clinically plausible — real CPT codes, real DICOM modality codes,
 * indications phrased the way an ED physician actually writes them — because the whole point of
 * this phase is to judge the workflow, and placeholder text makes a radiology worklist impossible
 * to evaluate. It is demo data about fictional people; it is not medical advice and no record here
 * describes a real patient.
 *
 * The names, MRNs, accession numbers, procedures, indications and report prose mirror the design
 * prototype's dataset so the Android build and the design mocks can be compared side by side.
 *
 * Everything is generated relative to [today] so the app never looks stale.
 */
data class ClinicalSeed(
    val doctor: Doctor,
    val facilities: List<Facility>,
    val patients: List<Patient>,
    val appointments: List<Appointment>,
    val orders: List<Order>,
    val studies: List<Study>,
    val reports: List<RadiologyReport>,
    val notifications: List<HelixNotification>,
    val hl7Messages: List<Hl7Message>,
    val channels: List<InterfaceChannel>,
)

@Suppress("LargeClass", "LongMethod")
object SeedClinicalData {

    private const val FACILITY_MAIN = "MAIN"
    private const val FACILITY_NORTHSHORE = "NSHR"
    private const val FACILITY_EASTVIEW = "EAST"

    fun build(today: LocalDate = LocalDate.now()): ClinicalSeed {
        fun at(hour: Int, minute: Int, second: Int = 0): LocalDateTime =
            LocalDateTime.of(today, LocalTime.of(hour, minute, second))

        /** Keeps the prototype's birthday (month/day) while pinning the age to [age] as of today. */
        fun dob(month: Int, day: Int, age: Int): LocalDate {
            val birthdayThisYear = LocalDate.of(today.year, month, day)
            val year = if (birthdayThisYear.isAfter(today)) today.year - age - 1 else today.year - age
            return LocalDate.of(year, month, day)
        }

        val doctor = Doctor(
            id = "DR1284",
            displayName = "Dr. Sarah Chen",
            specialty = "Diagnostic Radiology",
            facility = "Mayo Regional · Main",
            licenceIdentifier = "NPI 1245789632",
            isOnCall = true,
            initials = "SC",
        )

        val facilities = listOf(
            Facility(FACILITY_MAIN, "Mayo Regional · Main", "Main"),
            Facility(FACILITY_NORTHSHORE, "Northshore Imaging", "Northshore"),
            Facility(FACILITY_EASTVIEW, "Eastview Outpatient", "Eastview"),
        )

        val patients = listOf(
            Patient(
                id = "pat-park",
                fullName = "Linda Park",
                mrn = "MRN8859214",
                age = 76,
                sex = Sex.FEMALE,
                dateOfBirth = dob(7, 19, 76),
                allergies = listOf("Penicillin — rash", "Iodinated contrast — mild urticaria"),
                history = listOf(
                    problem("h-park-1", today.minusYears(7), "Atrial fibrillation"),
                    problem("h-park-2", today.minusYears(9), "Hypertension"),
                    problem("h-park-3", today.minusYears(3), "Prior TIA"),
                    prior("h-park-4", today.minusMonths(30), "MR", "MRI Brain w/o Contrast", "Chronic microvascular ischemic change. No acute infarct."),
                    prior("h-park-5", today.minusMonths(59), "CT", "CT Head w/o Contrast", "No acute intracranial abnormality."),
                ),
            ),
            Patient(
                id = "pat-kowalski",
                fullName = "Theresa Kowalski",
                mrn = "MRN8849031",
                age = 54,
                sex = Sex.FEMALE,
                dateOfBirth = dob(1, 8, 54),
                allergies = listOf("Iodinated contrast — moderate reaction (premedicate)"),
                history = listOf(
                    problem("h-kow-1", today.minusYears(2), "CKD stage 3 (eGFR 44)"),
                    problem("h-kow-2", today.minusYears(6), "Type 2 diabetes"),
                    prior("h-kow-3", today.minusMonths(15), "US", "US Abdomen Complete", "Cholelithiasis without cholecystitis."),
                ),
            ),
            Patient(
                id = "pat-reyes",
                fullName = "Marcus T. Reyes",
                mrn = "MRN8841902",
                age = 44,
                sex = Sex.MALE,
                dateOfBirth = dob(9, 23, 44),
                allergies = listOf("NKDA"),
                history = listOf(
                    problem("h-reyes-1", today.minusYears(8), "Right knee arthroscopy"),
                    prior("h-reyes-2", today.minusMonths(21), "XR", "XR Knee, 3 Views", "Mild medial compartment narrowing."),
                ),
            ),
            Patient(
                id = "pat-hartwell",
                fullName = "Eleanor J. Hartwell",
                mrn = "MRN8843261",
                age = 68,
                sex = Sex.FEMALE,
                dateOfBirth = dob(4, 12, 68),
                allergies = listOf("NKDA"),
                history = listOf(
                    problem("h-hart-1", today.minusMonths(12), "Screening mammography, annual"),
                    prior("h-hart-2", today.minusMonths(12), "MG", "Screening Mammography", "BI-RADS 1. Negative."),
                ),
            ),
            Patient(
                id = "pat-garrison",
                fullName = "Allen Garrison",
                mrn = "MRN8857702",
                age = 71,
                sex = Sex.MALE,
                dateOfBirth = dob(3, 2, 71),
                allergies = listOf("NKDA"),
                history = listOf(
                    problem("h-gar-1", today.minusYears(2), "Anticoagulated — apixaban"),
                    problem("h-gar-2", today.minusMonths(9), "Falls, recurrent"),
                ),
            ),
            Patient(
                id = "pat-olsen",
                fullName = "Daniel R. Olsen",
                mrn = "MRN8846712",
                age = 60,
                sex = Sex.MALE,
                dateOfBirth = dob(11, 30, 60),
                allergies = listOf("NKDA"),
                history = listOf(
                    problem("h-olsen-1", today.minusYears(5), "COPD"),
                    problem("h-olsen-2", today.minusYears(10), "40 pack-year smoking history"),
                ),
            ),
            Patient(
                id = "pat-brooks",
                fullName = "Jamal Brooks",
                mrn = "MRN8852399",
                age = 36,
                sex = Sex.MALE,
                dateOfBirth = dob(3, 4, 36),
                allergies = listOf("NKDA"),
                history = listOf(
                    problem("h-brooks-1", today.minusYears(3), "Chronic low back pain"),
                ),
            ),
            Patient(
                id = "pat-delgado",
                fullName = "Carmen Delgado",
                mrn = "MRN8854882",
                age = 57,
                sex = Sex.FEMALE,
                dateOfBirth = dob(8, 15, 57),
                allergies = listOf("Gadolinium — prior mild reaction"),
                history = listOf(
                    problem("h-del-1", today.minusMonths(6), "Hepatic lesion under surveillance"),
                    prior("h-del-2", today.minusMonths(3), "US", "US Abdomen Complete", "2.1cm hepatic lesion, segment VI. Recommend MRI."),
                ),
            ),
            Patient(
                id = "pat-demir",
                fullName = "Yusuf Demir",
                mrn = "MRN8862004",
                age = 38,
                sex = Sex.MALE,
                dateOfBirth = dob(2, 19, 38),
                allergies = listOf("NKDA"),
            ),
            Patient(
                id = "pat-sato",
                fullName = "Naomi Sato",
                mrn = "MRN8848117",
                age = 70,
                sex = Sex.FEMALE,
                dateOfBirth = dob(12, 11, 70),
                allergies = listOf("NKDA"),
                history = listOf(
                    problem("h-sato-1", today.minusYears(15), "Cholecystectomy"),
                ),
            ),
            Patient(
                id = "pat-iyer",
                fullName = "Priya Iyer",
                mrn = "MRN8851188",
                age = 39,
                sex = Sex.FEMALE,
                dateOfBirth = dob(10, 7, 39),
                allergies = listOf("NKDA"),
            ),
            Patient(
                id = "pat-mendoza",
                fullName = "George Mendoza",
                mrn = "MRN8842004",
                age = 73,
                sex = Sex.MALE,
                dateOfBirth = dob(5, 14, 73),
                allergies = listOf("NKDA"),
                history = listOf(
                    problem("h-mend-1", today.minusYears(2), "Stage IB NSCLC, s/p lobectomy"),
                    prior("h-mend-2", today.minusMonths(3), "CT", "CT Chest w/o Contrast", "No evidence of recurrence. Post-surgical change RUL."),
                ),
            ),
            Patient(
                id = "pat-zhao",
                fullName = "Mei Lin Zhao",
                mrn = "MRN8855401",
                age = 63,
                sex = Sex.FEMALE,
                dateOfBirth = dob(8, 9, 63),
                allergies = listOf("NKDA"),
                history = listOf(
                    problem("h-zhao-1", today.minusMonths(4), "Migraine with aura"),
                ),
            ),
            Patient(
                id = "pat-okonkwo",
                fullName = "Adaeze Okonkwo",
                mrn = "MRN8858220",
                age = 46,
                sex = Sex.FEMALE,
                dateOfBirth = dob(5, 21, 46),
                allergies = listOf("NKDA"),
            ),
            Patient(
                id = "pat-whitman",
                fullName = "Henry Whitman",
                mrn = "MRN8843991",
                age = 67,
                sex = Sex.MALE,
                dateOfBirth = dob(2, 28, 67),
                allergies = listOf("NKDA"),
            ),
            Patient(
                id = "pat-bradshaw",
                fullName = "Owen Bradshaw",
                mrn = "MRN8849990",
                age = 30,
                sex = Sex.MALE,
                dateOfBirth = dob(2, 4, 30),
                allergies = listOf("NKDA"),
            ),
        )

        val byId = patients.associateBy { it.id }
        fun p(id: String): Patient = requireNotNull(byId[id]) { "Unknown seed patient $id" }

        val appointments = listOf(
            // ── Yesterday: the exams behind today's signed reports. ──
            appointment("APT-83094", p("pat-brooks"), at(15, 0).minusDays(1), 30, Modality.MR, "MRI Lumbar Spine w/o Contrast", Priority.ROUTINE, AppointmentStatus.COMPLETED, FACILITY_MAIN, "MR-02"),
            appointment("APT-83102", p("pat-mendoza"), at(15, 45).minusDays(1), 45, Modality.CT, "CT Chest w/o Contrast", Priority.ROUTINE, AppointmentStatus.COMPLETED, FACILITY_MAIN, "CT-01"),
            appointment("APT-83108", p("pat-bradshaw"), at(16, 30).minusDays(1), 30, Modality.CT, "CT Abdomen & Pelvis w/ Contrast", Priority.URGENT, AppointmentStatus.COMPLETED, FACILITY_MAIN, "CT-01"),
            // ── Today. ──
            appointment("APT-83214", p("pat-hartwell"), at(8, 0), 30, Modality.MG, "Screening Mammography, Bilateral", Priority.ROUTINE, AppointmentStatus.COMPLETED, FACILITY_MAIN, "MG-01"),
            appointment("APT-83218", p("pat-reyes"), at(8, 30), 30, Modality.MR, "MRI Knee w/o Contrast", Priority.ROUTINE, AppointmentStatus.COMPLETED, FACILITY_MAIN, "MR-02"),
            appointment("APT-83227", p("pat-kowalski"), at(9, 15), 45, Modality.CT, "CT Abdomen & Pelvis w/ Contrast", Priority.URGENT, AppointmentStatus.CHECKED_IN, FACILITY_MAIN, "CT-01"),
            appointment("APT-83229", p("pat-okonkwo"), at(9, 15), 30, Modality.US, "US Abdomen, Complete", Priority.ROUTINE, AppointmentStatus.IN_PROGRESS, FACILITY_MAIN, "US-01"),
            appointment("APT-83231", p("pat-olsen"), at(9, 30), 15, Modality.XR, "XR Chest, 2 Views", Priority.ROUTINE, AppointmentStatus.IN_PROGRESS, FACILITY_NORTHSHORE, "XR-03"),
            appointment("APT-83232", p("pat-zhao"), at(9, 30), 45, Modality.CT, "CT Head/Brain w/o Contrast", Priority.ROUTINE, AppointmentStatus.SCHEDULED, FACILITY_MAIN, "CT-02"),
            appointment("APT-83248", p("pat-park"), at(10, 0), 60, Modality.MR, "MRI Brain w/ & w/o Contrast", Priority.STAT, AppointmentStatus.COMPLETED, FACILITY_MAIN, "MR-01"),
            appointment("APT-83249", p("pat-bradshaw"), at(10, 15), 30, Modality.XR, "XR Chest, 2 Views", Priority.ROUTINE, AppointmentStatus.SCHEDULED, FACILITY_MAIN, "XR-01"),
            appointment("APT-83255", p("pat-brooks"), at(11, 15), 30, Modality.MR, "MRI Lumbar Spine w/o Contrast", Priority.ROUTINE, AppointmentStatus.SCHEDULED, FACILITY_MAIN, "MR-02"),
            appointment("APT-83252", p("pat-demir"), at(11, 30), 30, Modality.US, "US Abdomen, Complete", Priority.URGENT, AppointmentStatus.SCHEDULED, FACILITY_EASTVIEW, "US-02"),
            appointment("APT-83260", p("pat-sato"), at(12, 0), 30, Modality.US, "US Abdomen, Complete", Priority.ROUTINE, AppointmentStatus.SCHEDULED, FACILITY_EASTVIEW, "US-02"),
            appointment("APT-83264", p("pat-garrison"), at(13, 30), 45, Modality.CT, "CT Head/Brain w/o Contrast", Priority.ROUTINE, AppointmentStatus.COMPLETED, FACILITY_MAIN, "CT-02"),
            appointment("APT-83270", p("pat-delgado"), at(14, 15), 60, Modality.MR, "MRI Abdomen w/ & w/o Contrast", Priority.URGENT, AppointmentStatus.SCHEDULED, FACILITY_MAIN, "MR-01"),
            appointment("APT-83278", p("pat-whitman"), at(15, 30), 30, Modality.XR, "XR Lumbosacral Spine, 2-3 Views", Priority.ROUTINE, AppointmentStatus.NO_SHOW, FACILITY_NORTHSHORE, "XR-01"),
            appointment("APT-83283", p("pat-iyer"), at(16, 0), 30, Modality.MG, "Screening Mammography, Bilateral", Priority.ROUTINE, AppointmentStatus.SCHEDULED, FACILITY_MAIN, "MG-02"),
            appointment("APT-83291", p("pat-mendoza"), at(16, 45), 45, Modality.CT, "CT Chest w/o Contrast", Priority.ROUTINE, AppointmentStatus.SCHEDULED, FACILITY_MAIN, "CT-01"),
            // ── Tomorrow, so the Schedule date selector has somewhere to move to. ──
            appointment("APT-83302", p("pat-whitman"), at(8, 30).plusDays(1), 30, Modality.XR, "XR Lumbosacral Spine, 2-3 Views", Priority.ROUTINE, AppointmentStatus.SCHEDULED, FACILITY_NORTHSHORE, "XR-01"),
            appointment("APT-83309", p("pat-zhao"), at(13, 0).plusDays(1), 45, Modality.MR, "MRI Brain w/o Contrast", Priority.ROUTINE, AppointmentStatus.SCHEDULED, FACILITY_MAIN, "MR-02"),
        )

        val orders = listOf(
            order("ORD-44128", "ACC-2026-19014", p("pat-park"), Modality.MR, "70553", "MRI Brain w/ & w/o Contrast",
                "AMS, 76 y/o F. Sudden onset R-sided weakness. R/O acute infarct vs hemorrhage.",
                "ED · Dr. Hill", "Emergency", Priority.STAT, OrderStatus.PENDING, at(9, 12), FACILITY_MAIN, "RPT-91204"),
            order("ORD-44131", "ACC-2026-19017", p("pat-kowalski"), Modality.CT, "74177", "CT Abdomen & Pelvis w/ Contrast",
                "RLQ pain 18h, fever 38.4, WBC 14.2, rebound tenderness. R/O appendicitis.",
                "Dr. Lindgren, K.", "Emergency", Priority.URGENT, OrderStatus.IN_PROGRESS, at(9, 4), FACILITY_MAIN, null),
            order("ORD-44119", "ACC-2026-19009", p("pat-garrison"), Modality.CT, "70450", "CT Head/Brain w/o Contrast",
                "Fall from standing. GCS 14. On apixaban. R/O intracranial hemorrhage.",
                "ED · Dr. Nakamura", "Emergency", Priority.STAT, OrderStatus.COMPLETED, at(8, 51), FACILITY_MAIN, "RPT-91198"),
            order("ORD-44141", "ACC-2026-19023", p("pat-demir"), Modality.US, "76700", "US Abdomen, Complete",
                "RUQ pain, elevated AST 142 / ALT 118. R/O cholecystitis.",
                "Dr. Hossain, F.", "Internal Medicine", Priority.URGENT, OrderStatus.PENDING, at(9, 22), FACILITY_EASTVIEW, null),
            order("ORD-44102", "ACC-2026-19004", p("pat-reyes"), Modality.MR, "73721", "MRI Knee w/o Contrast",
                "Chronic R knee pain 6 months. Mechanical symptoms. Suspected meniscal tear.",
                "Dr. Okafor, J.", "Orthopedics", Priority.ROUTINE, OrderStatus.COMPLETED, at(8, 30), FACILITY_MAIN, "RPT-91179"),
            order("ORD-44095", "ACC-2026-19044", p("pat-delgado"), Modality.MR, "74183", "MRI Abdomen w/ & w/o Contrast",
                "Hepatic lesion characterization, segment VI. Prior US showed 2.1cm lesion.",
                "Dr. Hossain, F.", "Hepatology", Priority.ROUTINE, OrderStatus.PENDING, at(8, 14), FACILITY_MAIN, null),
            order("ORD-44152", "ACC-2026-19022", p("pat-zhao"), Modality.CT, "70450", "CT Head/Brain w/o Contrast",
                "New-onset migraine with aura, age 63. R/O structural lesion.",
                "Dr. Wang, P.", "Neurology", Priority.ROUTINE, OrderStatus.PENDING, at(9, 31), FACILITY_MAIN, null),
            order("ORD-44158", "ACC-2026-19058", p("pat-mendoza"), Modality.CT, "71250", "CT Chest w/o Contrast",
                "Surveillance imaging. Stage IB NSCLC s/p RUL lobectomy.",
                "Dr. Lindgren, K.", "Oncology", Priority.ROUTINE, OrderStatus.PENDING, at(9, 40), FACILITY_MAIN, null),
            // ── Orders backing the earlier reads, so every report has a real order behind it. ──
            order("ORD-44087", "ACC-2026-19001", p("pat-hartwell"), Modality.MG, "77067", "Screening Mammography, Bilateral",
                "Annual screening. No symptoms.",
                "Dr. Patel, Aisha", "Breast Imaging", Priority.ROUTINE, OrderStatus.COMPLETED, at(7, 45), FACILITY_MAIN, "RPT-91187"),
            order("ORD-44012", "ACC-2026-18960", p("pat-mendoza"), Modality.CT, "71250", "CT Chest w/o Contrast",
                "Oncology surveillance. s/p RUL lobectomy. Assess for recurrence.",
                "Dr. Wang, P.", "Oncology", Priority.ROUTINE, OrderStatus.COMPLETED, at(15, 20).minusDays(1), FACILITY_MAIN, "RPT-91164"),
            order("ORD-44008", "ACC-2026-18955", p("pat-bradshaw"), Modality.CT, "74177", "CT Abdomen & Pelvis w/ Contrast",
                "RLQ pain, anorexia, WBC 15.8. R/O appendicitis.",
                "Dr. Lindgren, K.", "Emergency", Priority.URGENT, OrderStatus.COMPLETED, at(16, 5).minusDays(1), FACILITY_MAIN, "RPT-91155"),
            order("ORD-44001", "ACC-2026-18948", p("pat-brooks"), Modality.MR, "72148", "MRI Lumbar Spine w/o Contrast",
                "Chronic LBP with radiculopathy, failed conservative tx.",
                "Dr. Okafor, J.", "Orthopedics", Priority.ROUTINE, OrderStatus.COMPLETED, at(14, 40).minusDays(1), FACILITY_MAIN, "RPT-91142"),
        )

        val studies = orders.mapIndexed { index, order ->
            Study(
                id = "std-${index + 1}",
                accessionNumber = order.accessionNumber,
                orderId = order.id,
                patientId = order.patientId,
                modality = order.modality,
                description = order.procedure,
                performedAt = if (order.status == OrderStatus.PENDING) null else order.orderedAt.plusMinutes(35),
                seriesCount = when (order.modality) {
                    Modality.MR -> 8
                    Modality.CT -> 5
                    Modality.US -> 3
                    else -> 1
                },
                imageCount = when (order.modality) {
                    Modality.MR -> 412
                    Modality.CT -> 286
                    Modality.US -> 44
                    Modality.MG -> 4
                    else -> 2
                },
                studyInstanceUid = "1.2.840.113619.2.55.3.${604688119 + index}.${index + 1}",
            )
        }

        val studyByOrder = studies.associateBy { it.orderId }

        val reports = listOf(
            RadiologyReport(
                id = "RPT-91204",
                orderId = "ORD-44128",
                studyId = studyByOrder.getValue("ORD-44128").id,
                accessionNumber = "ACC-2026-19014",
                patientId = "pat-park",
                patientName = "Linda Park",
                patientAge = 76,
                patientSex = Sex.FEMALE,
                mrn = "MRN8859214",
                modality = Modality.MR,
                procedure = "MRI Brain w/ & w/o Contrast",
                clinicalIndication = "AMS, 76 y/o F. Sudden onset R-sided weakness. R/O acute infarct vs hemorrhage.",
                comparison = "MRI Brain ${ClinicalFormat.shortDate(today.minusMonths(30))}.",
                technique = "Multiplanar multisequence MRI of the brain was performed without and with intravenous gadolinium (Gadavist 7.5 mL). DWI, ADC, FLAIR, T1, T2, SWI, post-contrast T1.",
                findings = "Restricted diffusion involving the left middle cerebral artery territory including the insular cortex and lateral temporal lobe, with corresponding ADC hypointensity, consistent with acute infarction. No associated hemorrhagic transformation on SWI. No mass effect or midline shift. Chronic microvascular ischemic change, stable. Ventricles and sulci appropriate for age.",
                impression = "1. Acute left MCA territory infarct, without hemorrhagic transformation.\n2. Recommend urgent neurology consultation and stroke-protocol management.",
                priority = Priority.STAT,
                status = ReportStatus.PENDING_SIGNATURE,
                radiologist = "Dr. S. Chen",
                createdAt = at(9, 30),
                finalizedAt = at(9, 48),
                signedAt = null,
                isCritical = true,
            ),
            RadiologyReport(
                id = "RPT-91198",
                orderId = "ORD-44119",
                studyId = studyByOrder.getValue("ORD-44119").id,
                accessionNumber = "ACC-2026-19009",
                patientId = "pat-garrison",
                patientName = "Allen Garrison",
                patientAge = 71,
                patientSex = Sex.MALE,
                mrn = "MRN8857702",
                modality = Modality.CT,
                procedure = "CT Head/Brain w/o Contrast",
                clinicalIndication = "Fall from standing. GCS 14. On apixaban. R/O intracranial hemorrhage.",
                comparison = "None available.",
                technique = "Axial non-contrast CT of the head, 5mm reconstructions, bone and soft tissue algorithms.",
                findings = "No acute intracranial hemorrhage, mass effect, or midline shift. Grey-white differentiation preserved. Age-appropriate cerebral atrophy with commensurate ventricular prominence. No skull fracture. Visualized paranasal sinuses and mastoid air cells clear.",
                impression = "1. No acute intracranial hemorrhage or traumatic injury.\n2. Age-appropriate atrophy.",
                priority = Priority.STAT,
                status = ReportStatus.PENDING_SIGNATURE,
                radiologist = "Dr. S. Chen",
                createdAt = at(9, 8),
                finalizedAt = at(9, 21),
                signedAt = null,
                isCritical = false,
            ),
            RadiologyReport(
                id = "RPT-91187",
                orderId = "ORD-44087",
                studyId = studyByOrder.getValue("ORD-44087").id,
                accessionNumber = "ACC-2026-19001",
                patientId = "pat-hartwell",
                patientName = "Eleanor J. Hartwell",
                patientAge = 68,
                patientSex = Sex.FEMALE,
                mrn = "MRN8843261",
                modality = Modality.MG,
                procedure = "Screening Mammography, Bilateral",
                clinicalIndication = "Annual screening. No symptoms.",
                comparison = "Screening mammography ${ClinicalFormat.shortDate(today.minusMonths(12))}.",
                technique = "Bilateral digital screening mammography with tomosynthesis. CC and MLO projections.",
                findings = "Breast composition category B — scattered areas of fibroglandular density. No suspicious mass, architectural distortion, or malignant-type calcification in either breast. No interval change from prior.",
                impression = "BI-RADS 1 — Negative.\nRoutine annual screening recommended.",
                priority = Priority.ROUTINE,
                status = ReportStatus.PENDING_SIGNATURE,
                radiologist = "Dr. S. Chen",
                createdAt = at(8, 40),
                finalizedAt = at(8, 54),
                signedAt = null,
                isCritical = false,
            ),
            RadiologyReport(
                id = "RPT-91179",
                orderId = "ORD-44102",
                studyId = studyByOrder.getValue("ORD-44102").id,
                accessionNumber = "ACC-2026-19004",
                patientId = "pat-reyes",
                patientName = "Marcus T. Reyes",
                patientAge = 44,
                patientSex = Sex.MALE,
                mrn = "MRN8841902",
                modality = Modality.MR,
                procedure = "MRI Knee w/o Contrast",
                clinicalIndication = "Chronic R knee pain 6 months. Mechanical symptoms. Suspected meniscal tear.",
                comparison = "XR Knee ${ClinicalFormat.shortDate(today.minusMonths(21))}.",
                technique = "Multiplanar multisequence MRI of the right knee without intravenous contrast.",
                findings = "Oblique tear of the posterior horn of the medial meniscus extending to the inferior articular surface, grade 2–3. Cruciate and collateral ligaments intact. Small joint effusion. Mild chondral thinning of the medial femoral condyle. No fracture or marrow oedema.",
                impression = "1. Grade 2–3 tear, posterior horn medial meniscus.\n2. Small joint effusion.\n3. Mild medial compartment chondrosis.",
                priority = Priority.ROUTINE,
                status = ReportStatus.PRELIMINARY,
                radiologist = "Dr. S. Chen",
                createdAt = at(8, 42),
                finalizedAt = null,
                signedAt = null,
                isCritical = false,
            ),
            RadiologyReport(
                id = "RPT-91164",
                orderId = "ORD-44012",
                studyId = studyByOrder.getValue("ORD-44012").id,
                accessionNumber = "ACC-2026-18960",
                patientId = "pat-mendoza",
                patientName = "George Mendoza",
                patientAge = 73,
                patientSex = Sex.MALE,
                mrn = "MRN8842004",
                modality = Modality.CT,
                procedure = "CT Chest w/o Contrast",
                clinicalIndication = "Oncology surveillance. s/p RUL lobectomy. Assess for recurrence.",
                comparison = "CT Chest ${ClinicalFormat.shortDate(today.minusMonths(3))}.",
                technique = "Non-contrast helical CT of the chest, 1.25mm reconstructions.",
                findings = "Post-surgical change in the right upper lobe consistent with prior lobectomy. A 4mm solid nodule in the residual right upper lobe is unchanged from the prior study. No new pulmonary nodule, mediastinal lymphadenopathy, or pleural effusion.",
                impression = "1. Stable 4mm RUL nodule. Follow-up in 12 months per Fleischner criteria.\n2. No evidence of recurrent disease.",
                priority = Priority.ROUTINE,
                status = ReportStatus.SIGNED,
                radiologist = "Dr. S. Chen",
                createdAt = at(7, 46),
                finalizedAt = at(7, 58),
                signedAt = at(8, 6),
                isCritical = false,
            ),
            RadiologyReport(
                id = "RPT-91155",
                orderId = "ORD-44008",
                studyId = studyByOrder.getValue("ORD-44008").id,
                accessionNumber = "ACC-2026-18955",
                patientId = "pat-bradshaw",
                patientName = "Owen Bradshaw",
                patientAge = 30,
                patientSex = Sex.MALE,
                mrn = "MRN8849990",
                modality = Modality.CT,
                procedure = "CT Abdomen & Pelvis w/ Contrast",
                clinicalIndication = "RLQ pain, anorexia, WBC 15.8. R/O appendicitis.",
                comparison = "None.",
                technique = "Contrast-enhanced CT of the abdomen and pelvis, portal venous phase.",
                findings = "Dilated appendix measuring 11mm with periappendiceal fat stranding and mural hyperenhancement. No appendicolith. No abscess or free intraperitoneal air. Remainder of the abdomen and pelvis unremarkable.",
                impression = "1. Acute uncomplicated appendicitis.\n2. No abscess or perforation.",
                priority = Priority.URGENT,
                status = ReportStatus.SIGNED,
                radiologist = "Dr. S. Chen",
                createdAt = at(7, 20),
                finalizedAt = at(7, 34),
                signedAt = at(7, 41),
                isCritical = false,
            ),
            RadiologyReport(
                id = "RPT-91142",
                orderId = "ORD-44001",
                studyId = studyByOrder.getValue("ORD-44001").id,
                accessionNumber = "ACC-2026-18948",
                patientId = "pat-brooks",
                patientName = "Jamal Brooks",
                patientAge = 36,
                patientSex = Sex.MALE,
                mrn = "MRN8852399",
                modality = Modality.MR,
                procedure = "MRI Lumbar Spine w/o Contrast",
                clinicalIndication = "Chronic LBP with radiculopathy, failed conservative tx.",
                comparison = "None.",
                technique = "Multiplanar multisequence MRI of the lumbar spine without contrast.",
                findings = "Right paracentral disc protrusion at L4-L5 with mild narrowing of the right lateral recess and contact with the traversing right L5 nerve root. Disc desiccation at L4-L5 and L5-S1. No high-grade central canal stenosis. Conus terminates at L1.",
                impression = "1. L4-L5 right paracentral disc protrusion with mild right foraminal narrowing and L5 nerve root contact.\n2. Multilevel degenerative disc disease.",
                priority = Priority.ROUTINE,
                status = ReportStatus.SIGNED,
                radiologist = "Dr. S. Chen",
                createdAt = at(6, 58),
                finalizedAt = at(7, 11),
                signedAt = at(7, 19),
                isCritical = false,
            ),
        )

        val notifications = listOf(
            HelixNotification(
                "N-001", NotificationCategory.CRITICAL,
                "Critical finding ready to sign",
                "Linda Park · MRI Brain · Acute L MCA infarct",
                at(9, 48), false, NotificationTarget.Report("RPT-91204"),
            ),
            HelixNotification(
                "N-002", NotificationCategory.ORDERS,
                "STAT order placed",
                "Theresa Kowalski · CT Abdomen/Pelvis w/ contrast · R/O appendicitis",
                at(9, 22), false, NotificationTarget.OrderDetail("ORD-44131"),
            ),
            HelixNotification(
                "N-003", NotificationCategory.RESULTS,
                "STAT report awaiting sign-off",
                "Allen Garrison · CT Head · No acute hemorrhage",
                at(9, 21), false, NotificationTarget.Report("RPT-91198"),
            ),
            HelixNotification(
                "N-004", NotificationCategory.APPOINTMENTS,
                "Patient arrived",
                "Theresa Kowalski · CT-01 · 09:15",
                at(9, 15), true, NotificationTarget.OrderDetail("ORD-44131"),
            ),
            HelixNotification(
                "N-005", NotificationCategory.APPOINTMENTS,
                "Schedule change",
                "Carmen Delgado moved 14:15 → 14:30 (MR-01)",
                at(9, 5), true, NotificationTarget.AppointmentDetail("APT-83270"),
            ),
            HelixNotification(
                "N-006", NotificationCategory.RESULTS,
                "Report signed by colleague",
                "Dr. Wang signed RPT-91164 on your behalf · CT Chest",
                at(8, 54), true, NotificationTarget.Report("RPT-91164"),
            ),
            HelixNotification(
                "N-007", NotificationCategory.SYSTEM,
                "Interface NACK",
                "RAD-OUT-ORU · ORU^R01 rejected by EpicCare · invalid PV1-3",
                at(9, 41), true, NotificationTarget.Integration,
            ),
            HelixNotification(
                "N-008", NotificationCategory.SYSTEM,
                "On-call assignment",
                "You are on-call 19:00 today → 07:00 tomorrow",
                at(8, 30), true, null,
            ),
            HelixNotification(
                "N-009", NotificationCategory.ORDERS,
                "Allergy flag on contrast order",
                "Theresa Kowalski · iodinated contrast — moderate reaction. Premedication required.",
                at(9, 6), true, NotificationTarget.PatientDetail("pat-kowalski"),
            ),
        )

        val hl7Messages = listOf(
            Hl7Message("MSG-93421", Hl7MessageType.ORU, at(9, 47, 12), Hl7AckStatus.ACK, "Result · MRI Brain · Linda Park", "RAD-OUT-ORU"),
            Hl7Message("MSG-93418", Hl7MessageType.SIU, at(9, 46, 51), Hl7AckStatus.ACK, "Schedule · New appointment · Jamal Brooks", "EPIC-IN-SIU"),
            Hl7Message("MSG-93410", Hl7MessageType.ORM, at(9, 46, 4), Hl7AckStatus.ACK, "Order · CT Abdomen/Pelvis · Theresa Kowalski", "EPIC-IN-ORM"),
            Hl7Message("MSG-93404", Hl7MessageType.ADT, at(9, 45, 31), Hl7AckStatus.ACK, "Patient registration · Yusuf Demir", "EPIC-IN-ADT"),
            Hl7Message("MSG-93398", Hl7MessageType.ORU, at(9, 44, 18), Hl7AckStatus.PENDING, "Result · MRI Knee · Marcus T. Reyes", "RAD-OUT-ORU"),
            Hl7Message("MSG-93386", Hl7MessageType.ORM, at(9, 43, 55), Hl7AckStatus.ACK, "Order · MRI L-Spine · Jamal Brooks", "EPIC-IN-ORM"),
            Hl7Message("MSG-93377", Hl7MessageType.SIU, at(9, 42, 30), Hl7AckStatus.ACK, "Schedule · Modified · Carmen Delgado", "EPIC-IN-SIU"),
            Hl7Message("MSG-93371", Hl7MessageType.ORU, at(9, 41, 9), Hl7AckStatus.NAK, "Result · XR Chest · Daniel R. Olsen", "RAD-OUT-ORU"),
            Hl7Message("MSG-93368", Hl7MessageType.ADT, at(9, 40, 44), Hl7AckStatus.ACK, "Patient update · Naomi Sato", "EPIC-IN-ADT"),
            Hl7Message("MSG-93360", Hl7MessageType.SIU, at(9, 39, 22), Hl7AckStatus.ACK, "Schedule · New appointment · Priya Iyer", "EPIC-IN-SIU"),
            Hl7Message("MSG-93345", Hl7MessageType.ORM, at(9, 37, 48), Hl7AckStatus.ACK, "Order · US Abdomen · Yusuf Demir", "EPIC-IN-ORM"),
        )

        val channels = listOf(
            InterfaceChannel("ch-01", "EPIC-IN-ADT", InterfaceState.ONLINE, 412, at(9, 45, 31)),
            InterfaceChannel("ch-02", "EPIC-IN-SIU", InterfaceState.ONLINE, 268, at(9, 46, 51)),
            InterfaceChannel("ch-03", "EPIC-IN-ORM", InterfaceState.ONLINE, 221, at(9, 46, 4)),
            InterfaceChannel("ch-04", "RAD-OUT-ORU", InterfaceState.DEGRADED, 175, at(9, 47, 12)),
            InterfaceChannel("ch-05", "PACS-MDM", InterfaceState.ONLINE, 69, at(9, 38, 11)),
            InterfaceChannel("ch-06", "CERNER-IN-ADT", InterfaceState.OFFLINE, 13, at(7, 2, 0)),
            InterfaceChannel("ch-07", "MWL-SYNC", InterfaceState.ONLINE, 59, at(9, 50, 0)),
        )

        return ClinicalSeed(
            doctor = doctor,
            facilities = facilities,
            patients = patients,
            appointments = appointments,
            orders = orders,
            studies = studies,
            reports = reports,
            notifications = notifications,
            hl7Messages = hl7Messages,
            channels = channels,
        )
    }

    /** A problem-list entry: the prototype carries these as plain strings. */
    private fun problem(id: String, date: LocalDate, summary: String) =
        HistoryEntry(id, date, summary, "Active problem list entry.")

    /** A prior imaging study, summarised the way it appears on the relevant-priors rail. */
    private fun prior(id: String, date: LocalDate, modality: String, procedure: String, finding: String) =
        HistoryEntry(id, date, "$modality · $procedure", finding)

    private fun appointment(
        id: String,
        patient: Patient,
        start: LocalDateTime,
        durationMinutes: Int,
        modality: Modality,
        procedure: String,
        priority: Priority,
        status: AppointmentStatus,
        facilityId: String,
        room: String,
    ) = Appointment(
        id = id,
        patientId = patient.id,
        patientName = patient.fullName,
        patientAge = patient.age,
        patientSex = patient.sex,
        mrn = patient.mrn,
        start = start,
        durationMinutes = durationMinutes,
        modality = modality,
        procedure = procedure,
        priority = priority,
        status = status,
        facilityId = facilityId,
        room = room,
    )

    @Suppress("LongParameterList")
    private fun order(
        id: String,
        accession: String,
        patient: Patient,
        modality: Modality,
        cpt: String,
        procedure: String,
        indication: String,
        orderingPhysician: String,
        location: String,
        priority: Priority,
        status: OrderStatus,
        orderedAt: LocalDateTime,
        facilityId: String,
        reportId: String?,
    ) = Order(
        id = id,
        accessionNumber = accession,
        patientId = patient.id,
        patientName = patient.fullName,
        patientAge = patient.age,
        patientSex = patient.sex,
        mrn = patient.mrn,
        modality = modality,
        cptCode = cpt,
        procedure = procedure,
        clinicalIndication = indication,
        orderingPhysician = orderingPhysician,
        orderingLocation = location,
        priority = priority,
        status = status,
        orderedAt = orderedAt,
        facilityId = facilityId,
        reportId = reportId,
    )
}
