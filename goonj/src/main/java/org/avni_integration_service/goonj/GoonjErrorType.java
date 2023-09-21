package org.avni_integration_service.goonj;

public enum GoonjErrorType {
    DemandDeletionFailure, DispatchDeletionFailure,
    DispatchLineItemsDeletionFailure, UpdateDispatchReceiptError,
    DeleteEntityError, EntityIsDeleted,
    AddressNotFoundError, DemandAttributesMismatch,
    DispatchAttributesMismatch, DispatchReceiptAttributesMismatch,
    DistributionAttributesMismatch, ActivityAttributesMismatch,
    ImplementationInventoryAttributesMismatch, UnclassifiedError;

    public static GoonjErrorType safeGetValueOf(String stringValue, GoonjErrorType fallback) {
        for (GoonjErrorType ge : GoonjErrorType.values()) {
            if (ge.name().equalsIgnoreCase(stringValue))
                return ge;
        }
        return fallback;
    }
}
