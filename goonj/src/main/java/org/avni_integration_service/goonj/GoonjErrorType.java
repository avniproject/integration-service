package org.avni_integration_service.goonj;

public enum GoonjErrorType {
    DemandDeletionFailure, DispatchDeletionFailure,
    DispatchLineItemsDeletionFailure, UpdateDispatchReceiptError,
    DeleteEntityError, EntityIsDeleted,
    AddressNotFoundError, DemandAttributesMismatch,
    DispatchAttributesMismatch, DispatchReceiptAttributesMismatch,
    DistributionAttributesMismatch, ActivityAttributesMismatch,
    ImplementationInventoryAttributesMismatch, UnclassifiedError,
    BadValueForRestrictedPicklist, MustNotHave2SimilarElements,
    FieldCustomValidationException, AnswerMappingIsNull,
    TargetCommunityIsNullError, ClassCastException,
    InvalidAddressError, AnswerMappingNotFoundForCodedConcept,
    BadGateway;

    public static GoonjErrorType safeGetValueOf(String stringValue, GoonjErrorType fallback) {
        for (GoonjErrorType ge : GoonjErrorType.values()) {
            if (ge.name().equalsIgnoreCase(stringValue))
                return ge;
        }
        return fallback;
    }
}
