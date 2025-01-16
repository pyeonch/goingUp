package GoingUp.Features;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class StockBuyStatus {
    private final  boolean canProceed;
    private final int buyableStocks;

}
