package GoingUp.Features;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StockBuyStatus {
    private final  String failCause;
    private final int buyableStocks;

}
