package GoingUp;

import GoingUp.Features.Players;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

import static GoingUp.Consts.*;

@Slf4j
@RequiredArgsConstructor
public class GoingUp extends ListenerAdapter {

    private final HashMap<Member, Players> joinUsers = new HashMap<>();
    public int currentRound = 1;
    public String currentPhase = "시작 전";
    public String ADMIN_CONSOLE_STATUS_MESSAGE_ID = "";

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if(event.getAuthor().isBot()) return;
        if(!event.isFromGuild()) return;

        Channel channel = event.getChannel();
        if(channel instanceof TextChannel textChannel) {
            String[] message = event.getMessage().getContentRaw().split(" ");
            if(textChannel.getId().equals(TC_ADMIN_CONSOLE_ID) && message[0].equals("!콘솔")){
                adminConsoleButtons(textChannel);
            }
        }
    }

    public void adminConsoleButtons(TextChannel textChannel) {
        textChannel.sendMessage("게임 관리 콘솔입니다.").addActionRow(
                Button.secondary("join_player","플레이어 모집"),
                Button.success("game_start","게임 시작")
        ).addActionRow(
                Button.primary("buy_sell","구매/판매"),
                Button.primary("close_market","장마감"),
                Button.primary("select_news","기사선택"),
                Button.primary("rest","휴식")
        ).addActionRow(
                Button.success("call_player","전체소집"),
                Button.danger("reset_game","초기화")
        ).queue();

        if(ADMIN_CONSOLE_STATUS_MESSAGE_ID.isEmpty()) {
            textChannel.sendMessage(">>> 현재 라운드 : " + currentRound + "\n" +
                    "현재 페이즈 : " + currentPhase).queue(message -> ADMIN_CONSOLE_STATUS_MESSAGE_ID = message.getId());
        } else {
            textChannel.retrieveMessageById(ADMIN_CONSOLE_STATUS_MESSAGE_ID).queue(message ->
                    message.editMessage(">>> 현재 라운드 : " + currentRound + "\n" +
                            "현재 페이즈 : " + currentPhase).queue()
            );

        }
    }

    private void joinPlayerButtons(TextChannel textChannel) {
        textChannel.sendMessage("반갑습니다! 플레이어분들, 하단의 플레이어버튼을 선택해주세요.").queue();
    }

    // 유저에게 역할 권한 부여
    private void assignRole(Member member, Role roleToAssign) {
        if (roleToAssign != null) {
            member.getGuild().addRoleToMember(member, roleToAssign).queue();
        }
    }

    // 유저에게 역할 권한을 제거
    private void removeRole(Member member, Role role) {
        if (role != null) {
            member.getGuild().removeRoleFromMember(member, role).queue();
        }
    }
}