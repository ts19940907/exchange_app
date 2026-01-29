package com.myorg;

import java.util.Map;

public class ExchangeRecord {
    // APIのレスポンス形式に合わせたデータモデル
    public record ExchangeRateResponse(
            String result,
            String base_code,
            Map<String, Double> conversion_rates, // 各通貨のレートが入る
            long time_last_update_unix        // 最終更新時間
    ) {}
}
