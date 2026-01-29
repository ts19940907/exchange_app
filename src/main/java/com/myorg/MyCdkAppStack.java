package com.myorg;

import software.amazon.awscdk.services.events.Rule;
import software.amazon.awscdk.services.events.Schedule;
import software.amazon.awscdk.services.events.targets.LambdaFunction;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.s3.Bucket;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.lambda.Runtime;
// import software.amazon.awscdk.services.sqs.Queue;

public class MyCdkAppStack extends Stack {
    public MyCdkAppStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public MyCdkAppStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // 1. データを保存するためのS3バケットを作成
        Bucket exchangeBucket = Bucket.Builder.create(this, "ExchangeDataBucket")
                .versioned(true)
                .build();

        // 2. Lambda関数の定義
        Function exchangeLambda = Function.Builder.create(this, "ExchangeRateCollector")
                .runtime(Runtime.JAVA_17) // Java 17 または 21 を指定
                .handler("com.myorg.ExchangeLambdaHandler") // 作成したハンドラクラスを指定
                .code(Code.fromAsset("target/my-cdk-app-0.1.jar")) // mvn packageで生成されるjar
                .timeout(Duration.seconds(30))
                .memorySize(512)
                .build();

        // 3. LambdaにS3バケットへの書き込み権限を与える (最小権限の原則)
        exchangeBucket.grantWrite(exchangeLambda);

        // 4. 定期実行の設定 (1時間おきに実行)
        // データエンジニアとして「継続的なデータ収集」を自動化していることをアピール
        Rule rule = Rule.Builder.create(this, "ScheduleRule")
                .schedule(Schedule.rate(Duration.hours(1)))
                .build();
        rule.addTarget(new LambdaFunction(exchangeLambda));
    }
}
