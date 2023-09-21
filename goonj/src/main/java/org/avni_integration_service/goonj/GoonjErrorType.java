package org.avni_integration_service.goonj;

public enum GoonjErrorType {
    AddressNotFoundError, EntityIsDeleted,
    DemandAttributesMismatch, DispatchAttributesMismatch, DispatchReceiptAttributesMismatch,
    DistributionAttributesMismatch, ActivityAttributesMismatch, ImplementationInventoryAttributesMismatch,
    DemandDeletionFailure, DispatchDeletionFailure, DispatchLineItemsDeletionFailure, UnclassifiedError;

    public static GoonjErrorType safeGetValueOf(String stringValue, GoonjErrorType fallback) {
        for (GoonjErrorType ge : GoonjErrorType.values()) {
            if (ge.name().equalsIgnoreCase(stringValue))
                return ge;
        }
        return fallback;
    }
}
