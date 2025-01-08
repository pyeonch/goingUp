package GoingUp;

import GoingUp.Features.Players;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    //어드민 콘솔 버튼
    private void adminConsoleButtons(TextChannel textChannel) {
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

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        Channel channel = event.getChannel();
        Member member = event.getMember();
        Guild guild = event.getGuild();

        if(channel instanceof TextChannel textChannel) {
            if(textChannel.getId().equals(TC_ADMIN_CONSOLE_ID)) {
                adminConsoleButtonInteract(event, buttonId, textChannel);
            } else if (textChannel.getId().equals(TC_SYSTEM_ID)) {

                //플레이어 참가 버튼
                if(buttonId.startsWith("player_")){
                    String playerNumber = buttonId.replace("player_", "");
                    Role spectatorRole = guild.getRoleById(ROLE_SPECTATOR);

                    if (member != null && spectatorRole != null) {
                        //이미 참여신청함
                        if (joinUsers.containsKey(member)) {
                            event.reply("이미 참가하였습니다.").setEphemeral(true).queue();
                            loggingChannel(guild, "### 위험: [" + member.getEffectiveName() + "] 중복 신청");
                        } else {
                            boolean hasSpectatorRole = member.getRoles().contains(spectatorRole);
                            if (hasSpectatorRole) {
                                event.reply("관전 역할으로는 참여할 수 없습니다.").setEphemeral(true).queue();
                                loggingChannel(guild, "### 위험: [" + member.getEffectiveName() + "] 관전 상태에서 참가 신청");
                            } else {
                                //String playerNickname = String.format("%02d. %s", playerNumber, member.getEffectiveName());
                                joinUsers.putIfAbsent(member, new Players());


                                List<ActionRow> updatedRows = new ArrayList<>();
                                for (ActionRow row : event.getMessage().getActionRows()) {
                                    updatedRows.add(ActionRow.of(
                                            row.getButtons().stream()
                                                    .map(button -> button.getId().equals(buttonId)
                                                            ? button.asDisabled() // 클릭한 버튼만 비활성화
                                                            : button) // 나머지 버튼 유지
                                                    .toList()));
                                }
                                event.getMessage().editMessageComponents(updatedRows).queue();

                                event.reply("참가 완료").setEphemeral(true).queue();
                                loggingChannel(guild, "["+member.getEffectiveName()+"] 참가 완료");
                            }
                        }
                    }
                }
            }

        }
    }

    //운영자콘솔 버튼 이벤트
    private void adminConsoleButtonInteract(ButtonInteractionEvent event, String buttonId, TextChannel textChannel) {
        switch (buttonId) {
            case "join_player":
                joinPlayerButtons(event);
                break;
            case "game_start":
                break;
            case "buy_sell":
                break;
            case "close_market":
                break;
            case "select_news":
                break;
            case "rest":
                break;
            case "call_player":
                break;
            case "reset_game":
                break;

        }
    }

    private void joinPlayerButtons(ButtonInteractionEvent event) {
        TextChannel systemChannel =event.getGuild().getTextChannelById(TC_SYSTEM_ID);
        systemChannel.sendMessage("반갑습니다! 플레이어분들, 하단의 플레이어버튼을 선택해주세요.").addActionRow(
                Button.primary("player_1","플레이어1"),
                Button.primary("player_2","플레이어2"),
                Button.primary("player_3","플레이어3"),
                Button.primary("player_4","플레이어4"),
                Button.primary("player_5","플레이어5")
                ).addActionRow(
                        Button.primary("player_6","플레이어6"))
                .queue();
    }

    // 로그 채널에 로그 기록
    private void loggingChannel(Guild guild, String message) {
        TextChannel logChannel = guild.getTextChannelById(TC_LOG_ID);
        if (logChannel != null) {
            logChannel.sendMessage(message).queue();
        }
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