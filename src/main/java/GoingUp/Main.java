package GoingUp;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import static GoingUp.Consts.*;

public class Main {
    static BootPath bootPath;

    public static void main(String[] args) {
        JDA jda = JDABuilder.createDefault(_TOKEN)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();

        jda.addEventListener(new GoingUp()); // Buttons 객체 전달
    }
}
