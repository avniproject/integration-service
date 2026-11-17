package org.avni_integration_service.tanuh.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class S3UrisTest {
    private static final String A = "s3://prod-user-media/tanuh_uat/a1.jpg";

    @Test
    public void everyShapeAvniHasStoredIsRead() {
        for (String url : List.of(
                "https://s3.ap-south-1.amazonaws.com/prod-user-media/tanuh_uat/a1.jpg",
                "https://prod-user-media.s3.ap-south-1.amazonaws.com/tanuh_uat/a1.jpg",
                "https://s3-ap-south-1.amazonaws.com/prod-user-media/tanuh_uat/a1.jpg",
                "https://s3.amazonaws.com/prod-user-media/tanuh_uat/a1.jpg",
                "https://prod-user-media.s3.amazonaws.com/tanuh_uat/a1.jpg")) {
            assertEquals(Optional.of(A), S3Uris.one(url), url);
        }
    }

    @Test
    public void theKeyIsDecodedAndTheQueryDropped() {
        assertEquals(Optional.of("s3://prod-user-media/tanuh_uat/a 1.jpg"),
                S3Uris.one("https://s3.ap-south-1.amazonaws.com/prod-user-media/tanuh_uat/a%201.jpg?X-Amz-Signature=x"));
    }

    @Test
    public void anAddressOutsideS3HasNone() {
        for (String url : Arrays.asList(
                "minio://avni-user-media/t/a.jpg",
                "http://localhost:9000/avni-user-media/a.jpg",
                "https://example.org/prod-user-media/a.jpg",
                "https://s3.ap-south-1.amazonaws.com/prod-user-media/",
                "", null)) {
            assertEquals(Optional.empty(), S3Uris.one(url), String.valueOf(url));
        }
    }

    @Test
    public void allKeepsTheOrderAndFailsOnOne() {
        String b = "https://s3.ap-south-1.amazonaws.com/prod-user-media/tanuh_uat/b2.jpg";
        String a = "https://s3.ap-south-1.amazonaws.com/prod-user-media/tanuh_uat/a1.jpg";
        assertEquals(Optional.of(List.of(A, "s3://prod-user-media/tanuh_uat/b2.jpg")), S3Uris.all(List.of(a, b)));
        assertEquals(Optional.empty(), S3Uris.all(List.of(a, "minio://avni-user-media/t/a.jpg")));
        assertEquals(Optional.of(List.of()), S3Uris.all(List.of()));
    }

    @Test
    public void aPhotoUsedTwiceIsSentOnce() {
        String a = "https://s3.ap-south-1.amazonaws.com/prod-user-media/tanuh_uat/a1.jpg";
        String b = "https://s3.ap-south-1.amazonaws.com/prod-user-media/tanuh_uat/b2.jpg";
        assertEquals(Optional.of(List.of(A, "s3://prod-user-media/tanuh_uat/b2.jpg")), S3Uris.all(List.of(a, b, a)));
        assertEquals(Optional.of(List.of(A)), S3Uris.all(List.of(a, a + "?X-Amz-Signature=x")));
    }

    // Avni's signing check looks only at a key's first folder, so "tanuh_uat/../other_org/x.jpg" passes it while naming
    // another organisation's photo. A key that walks out of its folder, or has an empty step, has no S3 location here.
    @Test
    public void aKeyThatStepsOutOfItsFolderHasNone() {
        for (String url : List.of(
                "https://s3.ap-south-1.amazonaws.com/prod-user-media/tanuh_uat/../other_org/a1.jpg",
                "https://s3.ap-south-1.amazonaws.com/prod-user-media/tanuh_uat%2F..%2Fother_org%2Fa1.jpg",
                "https://prod-user-media.s3.ap-south-1.amazonaws.com/tanuh_uat/../other_org/a1.jpg",
                "https://s3.ap-south-1.amazonaws.com/prod-user-media/tanuh_uat/./a1.jpg",
                "https://s3.ap-south-1.amazonaws.com/prod-user-media/tanuh_uat//a1.jpg",
                "https://s3.ap-south-1.amazonaws.com/prod-user-media/../tanuh_uat/a1.jpg")) {
            assertEquals(Optional.empty(), S3Uris.one(url), url);
        }
    }

    @Test
    public void aDotInsideANameIsKept() {
        assertEquals(Optional.of("s3://prod-user-media/tanuh_uat/a..1.jpg"),
                S3Uris.one("https://s3.ap-south-1.amazonaws.com/prod-user-media/tanuh_uat/a..1.jpg"));
    }
}
