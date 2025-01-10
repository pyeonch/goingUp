package GoingUp.Features;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Stock {
    PARK(new int[]{15000, 18000, 20000, 25000, 25000, 11000, 0, 0}, "1.시안테마파크"),
    CAPITAL(new int[]{24000, 19000, 14000, 9000, 8000, 12000, 14000, 16000}, "2.돈내놔캐피탈"),
    MCAR(new int[]{16000, 27000, 40000, 22000, 9000, 14000, 0, 0}, "3.막달려자동차"),
    TOUR(new int[]{10000, 7000, 13000, 21000, 16000, 21000, 16000, 0}, "4.두발로여행사"),
    EAT(new int[]{15000, 10000, 7000, 13000, 17000, 21000, 9000, 0}, "5.효심먹거리투어"),
    SCAR(new int[]{20000, 20000, 20000, 5000, 7000, 9000, 12000, 14000}, "6.신중자동차"),
    BANK(new int[]{14000, 22000, 31000, 24000, 31000, 23000, 37000, 4000}, "7.맡겨봐은행"),
    PHARMACY(new int[]{0, 20000, 25000, 35000, 40000, 25000, 19000, 31000}, "8.다살려제약"),
    DEATH(new int[]{0, 23000, 18000, 28000, 23000, 29000, 24000, 28000}, "9.에프터데스상조"),
    BUILD(new int[]{0, 0, 16000, 21000, 27000, 10000, 5000, 0}, "10.잘살아건설");

    int[] val;
    String path;

}
