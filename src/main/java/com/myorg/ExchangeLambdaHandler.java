package com.myorg;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ExchangeLambdaHandler implements RequestHandler<Object, String> {

    // AWS SDKのクライアントはハンドラの外で初期化するのがパフォーマンス上のベストプラクティスです
    private final S3Client s3 = S3Client.builder()
            .region(Region.AP_NORTHEAST_1)
            .build();

    // 本来は環境変数から取得しますが、まずは直接記述でテストしましょう
    private static final String API_URL = "https://v6.exchangerate-api.com/v6/21e94388e0c721be3333a6fa/latest/USD";
    private static final String BUCKET_NAME = "mycdkappstack-exchangedatabuckete8379d97-o5pthsouz1o0";

    @Override
    public String handleRequest(Object input, Context context) {
        try {
            // 1. データ取得
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(API_URL)).build();
            String jsonResponse = client.send(request, HttpResponse.BodyHandlers.ofString()).body();
            String cleanJson = jsonResponse.replaceAll("[\\r\\n]", "").replaceAll("\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*$)", "");

            // 2. 保存用のファイル名作成 (yyyy/MM/dd/HHmm.json)
            // これによりS3内でデータが整理（パーティショニング）されます
            String key = "raw-data/" + DateTimeFormatter.ofPattern("yyyy/MM/dd/HHmm")
                    .withZone(ZoneId.of("Asia/Tokyo"))
                    .format(Instant.now()) + ".json";

            // 3. S3への保存
            s3.putObject(PutObjectRequest.builder()
                            .bucket(BUCKET_NAME)
                            .key(key)
                            .contentType("application/json")
                            .build(),
                    RequestBody.fromString(cleanJson));

            return "Successfully stored exchange rate to " + key;

        } catch (Exception e) {
            context.getLogger().log("Error occurred: " + e.getMessage());
            return "Error";
        }
    }
}
