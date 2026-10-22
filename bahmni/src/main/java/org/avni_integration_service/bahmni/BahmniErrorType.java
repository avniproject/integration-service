package org.avni_integration_service.bahmni;

public enum BahmniErrorType {
    NoPatientWithId, PatientIdChanged, EntityIsDeleted, NotACommunityMember,
    NoSubjectWithId, SubjectIdChanged, MultipleSubjectsWithId, SubjectIdNull,
    // JSS: problems with the Bahmni ID on an Avni "Individual ID Update" form (reported to JSSCP to fix)
    BahmniPatientNotFound, MultipleBahmniPatientsWithId, BahmniIdOnMultiplePeople, BahmniPatientLinkedToAnotherPerson;
}
