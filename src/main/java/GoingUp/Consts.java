package GoingUp;

import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.List;

public class Consts {
    final static String _TOKEN = "";

    final static String TC_ADMIN_CONSOLE_ID; //관리자콘솔
    final static String TC_ADMIN_PRE_BUY_ID; //찌라시구매관리
    final static String TC_ADMIN_MAIN_BUY_ID; //구매-판매
    final static String TC_LOG_ID; //로그
    final static String TC_SYSTEM_ID; //시스템알림
    final static String TC_CHART_ID; //주가차트
    final static String TC_FREE_CHAT_ID; //자유채널
    final static String VC_MAIN_ID; //광장 보이스챗
    final static String CATE_WALLET_ID; //개인지갑 카테고리
    final static String ROLE_PLAYER_ID; //플레이어 ID
    final static String ROLE_SPECTATOR_ID; //관전 ID
    final static String ROLE_ADMIN_ID; //딜러 ID

    static {
        String serverEnv = System.getenv("SERVER_ENV");
        boolean IS_PROD = false;
        if(serverEnv != null) {
            try {
                System.out.println("SERVER ENV: " + serverEnv);
                IS_PROD = ServerEnv.valueOf(serverEnv.toUpperCase()).equals(ServerEnv.PRD);
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid SERVER_ENV value: " + serverEnv);
            }
        } else {
            System.err.println("SERVER ENV NOT SET");
        }

    final static String CATE_WALLET_ID = "";

    final static String ROLE_PLAYER_ID = "";
    final static String ROLE_SPECTATOR_ID = "";
    final static String ROLE_ADMIN_ID = "";

    final static List<String> COMPANYS_ROUND1 = Arrays.asList("시안테마파크", "돈내놔캐피탈", "막달려자동차", "두발로여행사", "효심먹거리투어", "신중자동차", "맡겨봐은행");
    final static List<String> COMPANYS_ROUND2 = Arrays.asList("시안테마파크", "돈내놔캐피탈", "막달려자동차", "두발로여행사", "효심먹거리투어", "신중자동차", "맡겨봐은행", "다살려제약", "애프터데스상조");
    final static List<String> COMPANYS_ALL = Arrays.asList("시안테마파크", "돈내놔캐피탈", "막달려자동차", "두발로여행사", "효심먹거리투어", "신중자동차", "맡겨봐은행", "다살려제약", "애프터데스상조", "잘살아건설");
    final static List<String> COMPANYS_ROUND7 = Arrays.asList("돈내놔캐피탈", "두발로여행사", "효심먹거리투어", "신중자동차", "맡겨봐은행", "다살려제약", "애프터데스상조", "잘살아건설");

    //찌라시 가격
    final static String PRE_BUY_PRICE_FIRSTHALF = "10000";
    final static String PRE_BUY_PRICE_SECONDHALF = "15000";
    final static String PRE_BUY_PRICE_FIRSTHALF_ADDON = "25000";
    final static String PRE_BUY_PRICE_SECONDHALF_ADDON = "35000";

    final static int TIME_ROUND = 1200;
    final static int TIME_REST = 300;

    @AllArgsConstructor
    public enum ServerEnv {
        PRD, STG;
    }

    @AllArgsConstructor
    public enum BootPath {
        LOCAL, DOCKER;
    }

    @AllArgsConstructor
    public enum Phase {
        READY("시작 전"),
        OPEN("장 오픈"),
        CLOSED("장 마감"),
        NEWS("기사선택"),
        REST("휴식/찌라시구매");

        String desc;

        String getDesc() {
            return desc;
        }
    }
}
