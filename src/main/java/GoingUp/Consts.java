package GoingUp;

import lombok.AllArgsConstructor;

public class Consts {
    final static String _TOKEN = "";

    final static String TC_ADMIN_CONSOLE_ID = "";
    final static String TC_ADMIN_PRE_BUY_ID = "";
    final static String TC_ADMIN_MAIN_BUY_ID = "";
    final static String TC_LOG_ID = "";
    final static String TC_SYSTEM_ID = "";

    final static String VC_MAIN_ID = "";

    final static String CATE_WALLET_ID = "";

    final static String ROLE_PLAYER_ID = "";
    final static String ROLE_SPECTATOR_ID = "";
    final static String ROLE_ADMIN_ID = "";


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
