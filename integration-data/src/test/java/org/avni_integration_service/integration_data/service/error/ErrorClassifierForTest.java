package org.avni_integration_service.integration_data.service.error;

import org.avni_integration_service.integration_data.context.ContextIntegrationSystem;
import org.avni_integration_service.integration_data.domain.IntegrationSystem;
import org.avni_integration_service.integration_data.domain.error.ErrorType;
import org.avni_integration_service.integration_data.repository.AbstractSpringTest;
import org.avni_integration_service.integration_data.repository.IntegrationSystemRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@Disabled
@SpringBootTest(classes = {ErrorClassifier.class, IntegrationSystemRepository.class})
public class ErrorClassifierForTest extends AbstractSpringTest implements ErrorClassifierForGoonjTestConstants {
        private final ErrorClassifier errorClassifier;
        private final ContextIntegrationSystem integrationSystem;

        @Autowired
        public ErrorClassifierForTest(ErrorClassifier errorClassifier, IntegrationSystemRepository integrationSystemRepository) {
                this.errorClassifier = errorClassifier;
                this.integrationSystem = new ContextIntegrationSystem(integrationSystemRepository
                        .findBySystemType(IntegrationSystem.IntegrationSystemType.Goonj));
        }

        @Test
        public void escapeMissingDemandError() {
                assertNull(errorClassifier.classify(integrationSystem, ERROR_MSG_STANDARD_SKIP));
        }

        @Test
        public void classifyInvalidValueForPicklistError() {
                assertNotNull(invokeClassifyForContains(ERROR_MSG_INVALID_VALUE_FOR_RESTRICTED_PICKLIST));
        }

        @Test
        public void classifyDispatchReceiptDuplicateError() {
                assertNotNull(invokeClassifyForContains(ERROR_MSG_DISPATCH_RECEIPT_DUPLICATE));
        }

        @Test
        public void classifyFieldCustomValidationError() {
                assertNotNull(invokeClassifyForContains(ERROR_MSG_FIELD_CUSTOM_VALIDATION_EXCEPTION));
        }

        @Test
        public void classifyDispatchReceiptInvalidReceivedDateError() {
                assertNotNull(invokeClassifyForContains(ERROR_MSG_DISPATCH_RECEIPT_INVALID_RECEIVED_DATE));
        }

        @Test
        public void classifyDispatchReceiptDuplicatesError() {
                assertNotNull(invokeClassifyForContains(ERROR_MSG_DISPATCH_RECEIPT_DUPLICATES));
        }

        @Test
        public void classifyErrorMsgDispatchReceiptLineItemMismatchError() {
                assertNotNull(invokeClassifyForContains(ERROR_MSG_DISPATCH_RECEIPT_LINE_ITEM_MISMATCH));
        }

        @Test
        public void classifyErrorMsgDistributionDisasterMissingError() {
                assertNotNull(invokeClassifyForContains(ERROR_MSG_DISTRIBUTION_DISASTER_MISSING));
        }

        @Test
        public void classifyErrorMsgActivityMeasurementTypeMissingError() {
                assertNotNull(invokeClassifyForContains(ERROR_MSG_ACTIVITY_MEASUREMENT_TYPE_MISSING));
        }

        @Test
        public void classifyErrorMsgAddressNotFoundError() {
                assertEquals("AddressNotFoundError" , errorClassifier.classify(integrationSystem, ERROR_MSG_DEMAND_ADDRESS_NOT_FOUND).getName());
                assertEquals("AddressNotFoundError" , errorClassifier.classify(integrationSystem, ERROR_MSG_DISPATCH_ADDRESS_NOT_FOUND,
                        true, "UnclassifiedError").getName());
                assertEquals("AddressNotFoundError" , errorClassifier.classify(integrationSystem, ERROR_MSG_DISPATCH_ADDRESS_MAP_NOT_FOUND_VALID,
                        true, "UnclassifiedError").getName());
                assertEquals("UnclassifiedError" , errorClassifier.classify(integrationSystem, ERROR_MSG_DISPATCH_ADDRESS_NOT_FOUND_INVALID,
                        true, "UnclassifiedError").getName());
        }

        private ErrorType invokeClassifyForContains(String errorMessage) {
                String buffetedMsg = String.format(STRING_FORMAT_BUFFETED_ERROR_MSG, errorMessage);
                return errorClassifier.classify(integrationSystem, buffetedMsg);
        }
}
