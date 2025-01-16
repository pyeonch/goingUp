package GoingUp.Features;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public class Players {

    public Players(String name) {
        this.name = name;
    }

    @Setter
    private String name;
    @Setter
    private String channelId = ""; //개인 채널 텍스트 채널 아이디

    private int val = 500000;

    private int stock_park = 0; //시안테마파크
    private int stock_capital = 0; //돈내놔캐피탈
    private int stock_MCar = 0; //막달려자동차
    private int stock_tour = 0; //두발로여행사
    private int stock_eat = 0; //효심먹거리투어
    private int stock_Scar = 0; //신중자동차
    private int stock_bank = 0; //맡겨봐은행

    private int stock_pharmacy = 0; //다살려제약
    private int stock_death = 0; //애프터데스상조
    private int stock_build = 0; //잘살아건설

    private String init_TId = ""; //기사선택, 찌라시선택 등 임시 텍스트 아이디
    @Setter
    private String wallet_TId = ""; //지갑 현황 텍스트 아이디

    private int selectionCount = 0;

    public void incrementSelection() {
        selectionCount++;
    }
    public void initSelection() {
        selectionCount = 0;
    }

    public void minusVal(int price) {
        this.val -= price;
    }
    public void  plusVal(int price) {
        this.val += price;
    }
    public void plusStock(int val, Stock stock) {
        switch (stock) {
            case PARK -> stock_park += val;
            case CAPITAL -> stock_capital += val;
            case MCAR -> stock_MCar += val;
            case TOUR -> stock_tour += val;
            case EAT -> stock_eat += val;
            case SCAR -> stock_Scar += val;
            case BANK -> stock_bank += val;
            case PHARMACY -> stock_pharmacy += val;
            case DEATH -> stock_death += val;
            case BUILD -> stock_build += val;
        }
    }
    public void minusStock(int val, Stock stock) {
        switch (stock) {
            case PARK -> stock_park -= val;
            case CAPITAL -> stock_capital -= val;
            case MCAR -> stock_MCar -= val;
            case TOUR -> stock_tour -= val;
            case EAT -> stock_eat -= val;
            case SCAR -> stock_Scar -= val;
            case BANK -> stock_bank -= val;
            case PHARMACY -> stock_pharmacy -= val;
            case DEATH -> stock_death -= val;
            case BUILD -> stock_build -= val;
        }
    }
}
