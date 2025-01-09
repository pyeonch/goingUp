package GoingUp;

import GoingUp.Features.Players;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static GoingUp.Consts.*;

@Slf4j
@RequiredArgsConstructor
public class GoingUp extends ListenerAdapter {

    private final HashMap<Member, Players> joinUsers = new HashMap<>();
    public int currentRound = 1;
    public Phase currentPhase = Phase.READY;
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
                Button.primary("start_market","장오픈"),
                Button.primary("close_market","장마감"),
                Button.primary("select_news","기사선택"),
                Button.primary("rest","휴식")
        ).addActionRow(
                Button.success("call_player","전체소집"),
                Button.danger("reset_game","초기화")
        ).queue();

        if(ADMIN_CONSOLE_STATUS_MESSAGE_ID.isEmpty()) {
            textChannel.sendMessage(">>> 현재 라운드 : " + currentRound + "\n" +
                    "현재 페이즈 : " + currentPhase.getDesc()).queue(message -> ADMIN_CONSOLE_STATUS_MESSAGE_ID = message.getId());
        } else {
            textChannel.retrieveMessageById(ADMIN_CONSOLE_STATUS_MESSAGE_ID).queue(message ->
                    message.editMessage(">>> 현재 라운드 : " + currentRound + "\n" +
                            "현재 페이즈 : " + currentPhase.getDesc()).queue()
            );

        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        Channel eventChannel = event.getChannel();
        Member member = event.getMember();
        Guild guild = event.getGuild();

        if(eventChannel instanceof TextChannel textChannel) {
            if(textChannel.getId().equals(TC_ADMIN_CONSOLE_ID)) {
                adminConsoleButtonInteract(event, buttonId, textChannel);
            } else if (textChannel.getId().equals(TC_SYSTEM_ID)) {

                //플레이어 참가 버튼
                if(buttonId.startsWith("player_")){
                    event.deferEdit().queue();

                    String playerNumber = buttonId.replace("player_", "");
                    Role spectatorRole = guild.getRoleById(ROLE_SPECTATOR_ID);

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

                                //버튼 비활성화
                                List<ActionRow> updatedRows = new ArrayList<>();
                                for (ActionRow row : event.getMessage().getActionRows()) {
                                    updatedRows.add(ActionRow.of(
                                            row.getButtons().stream()
                                                    .map(button -> button.getId().equals(buttonId)
                                                            ? button.asDisabled().withLabel(member.getEffectiveName()) // 클릭한 버튼만 비활성화
                                                            : button) // 나머지 버튼 유지
                                                    .toList()));
                                }
                                event.getMessage().editMessageComponents(updatedRows).queue();

                                //개인지갑 채널 생성
                                Category category = event.getGuild().getCategoryById(CATE_WALLET_ID);
                                Role adminRole = event.getGuild().getRoleById(ROLE_ADMIN_ID);

                                category.createTextChannel(member.getEffectiveName()) // 사용자 이름으로 텍스트 채널 생성
                                        .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null) // 사용자에게 보기 및 쓰기 권한
                                        .addPermissionOverride(adminRole, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null) // 운영자 역할에 권한 부여
                                        .addPermissionOverride(event.getGuild().getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL)) // 다른 사용자 접근 금지
                                        .queue(channel -> {
                                            // 채널 생성 성공 시 ID를 Players 객체에 저장
                                            Players player = joinUsers.get(member);
                                            if (player != null) {
                                                player.setWalletTCId(channel.getId()); // walletTCId에 채널 ID 저장
                                            }
                                        }, error -> {
                                            // 에러 처리
                                            loggingChannel(guild,"### 오류: 개인 지갑 채널을 생성하는 데 실패했습니다.");
                                        });

                                createMsgAndErase(textChannel, "["+member.getEffectiveName() + "]참가 완료!");
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
        event.deferEdit().queue();
        switch (buttonId) {
            case "join_player":
                joinPlayerButtons(event, textChannel);
                break;
            case "game_start":
                break;
            case "start_market":
                break;
            case "close_market":
                break;
            case "select_news":
                break;
            case "rest":
                break;
            case "call_player":
                movePlayerToMainChannel(textChannel);
                break;
            case "reset_game":
                event.getChannel().sendMessage("주의: 로그 채널 제외 모든 메세지가 초기화됩니다.\n" +
                                "개인지갑의 개인채널들, 주가차트 등 모든 정보가 삭제되므로\n" +
                                " 모든 게임이 종료되었을 때 초기화 하는걸 권장드립니다.\n" +
                                "\n### 정말 초기화하시겠습니까? (10초 뒤 이 메세지는 사라집니다.)")
                        .addActionRow(Button.danger("confirm_reset", "확인"))
                        .queue(message -> {
                            message.delete().queueAfter(10, TimeUnit.SECONDS);
                        });
                break;
            case "confirm_reset":
                resetGame(textChannel);
                break;

        }
    }

    private void joinPlayerButtons(ButtonInteractionEvent event, TextChannel textChannel) {
        TextChannel systemChannel =event.getGuild().getTextChannelById(TC_SYSTEM_ID);
        systemChannel.sendMessage("반갑습니다! 플레이어분들, 하단의 플레이어 버튼을 선택해주세요.").addActionRow(
                Button.primary("player_1","플레이어1"),
                Button.primary("player_2","플레이어2"),
                Button.primary("player_3","플레이어3"),
                Button.primary("player_4","플레이어4"),
                Button.primary("player_5","플레이어5")
                ).addActionRow(
                        Button.primary("player_6","플레이어6"))
                .queue();

        createMsgAndErase(textChannel, "플레이어 참가 버튼 생성 완료!");
    }

    private void movePlayerToMainChannel(TextChannel channel) {
        Guild guild = channel.getGuild();

        // 이동 대상 멤버 수 추적
        int totalMembers = joinUsers.size();
        AtomicInteger completedCount = new AtomicInteger(0);

        channel.sendMessage("이동 중...").queue(message -> {
            if(joinUsers.keySet().isEmpty()) {
                editMsgAndErase(message, "이동할 플레이어가 없습니다!");
                loggingChannel(guild, "### 위험: 전체소집, 이동할 플레이어가 없음");
            }

            for (Member member : joinUsers.keySet()) {
                if (member.getVoiceState() == null || member.getVoiceState().getChannel() == null) {
                    guild.retrieveMemberById(member.getId()).queue(refreshedMember -> {
                        handleMemberVoiceChannel(channel, refreshedMember, guild);
                    });
                } else {
                    handleMemberVoiceChannel(channel, member, guild);
                }
                if(totalMembers == completedCount.incrementAndGet()) {
                    editMsgAndErase(message, "이동 완료!");
                    loggingChannel(guild, "전체 소집 완료");
                }
            }
        });

    }

    private void handleMemberVoiceChannel(TextChannel channel, Member member, Guild guild) {
        VoiceChannel mainChannel = guild.getVoiceChannelById(VC_MAIN_ID);

        if( member.getVoiceState() == null || member.getVoiceState().getChannel() == null) {
            loggingChannel(guild, "### 위험: ["+member.getEffectiveName()+"]님은 음성채널에 있지 않습니다.");
            return;
        }

        if(mainChannel != null) {
            guild.moveVoiceMember(member, mainChannel).queue(
                    success -> {
                        loggingChannel(guild, "["+member.getEffectiveName()+"] 이동 성공");
                    },
                    error -> {
                        loggingChannel(guild, "### 오류: 알 수 없는 이유로 ["+member.getEffectiveName()+"]님을 이동시키지 못했습니다.");
                    }
            );
        }
    }

    //게임 초기화 로직
    private void resetGame(TextChannel textChannel) {
        currentRound = 1;
        currentPhase = Phase.READY;

        Guild guild = textChannel.getGuild();

        initTextChannel(guild, TC_SYSTEM_ID);

        //개인채널삭제
        for (int i=0;i<joinUsers.size();i++) {
            Players player = new ArrayList<>(joinUsers.values()).get(i);
            Member member = new ArrayList<>(joinUsers.keySet()).get(i);

            TextChannel targetChannel = guild.getTextChannelById(player.getChannelId());

            if(targetChannel != null) {
                targetChannel.delete().queue(
                        success -> {
                            loggingChannel(guild, "["+member.getEffectiveName()+"] 채널 삭제 성공");
                        },
                        error -> {
                            loggingChannel(guild, "### 오류: 알 수 없는 이유로 ["+member.getEffectiveName()+"]의 채널을 삭제하지 못했습니다.");
                        });
            }
        }

        //주가차트 초기화 후 0회차 사진 올리기
        //딜러콘솔3개 초기화후 콘솔 버튼 올리기

        joinUsers.clear();
    }


    //=============================================================================================
    //메세지 생성 후 3초 후 삭제
    private void createMsgAndErase(TextChannel channel, String msg) {
        channel.sendMessage(msg).queue(message -> {
            message.delete().queueAfter(3, TimeUnit.SECONDS);
        });
    }

    //메세지 수정 및 3초 후 삭제
    private void editMsgAndErase(Message message, String replyText) {
        message.editMessage(replyText).queue(editedMessage -> {
            editedMessage.delete().queueAfter(3, TimeUnit.SECONDS);
        });
    }

    // 로그 채널에 로그 기록
    private void loggingChannel(Guild guild, String message) {
        TextChannel logChannel = guild.getTextChannelById(TC_LOG_ID);
        if (logChannel != null) {
            logChannel.sendMessage(message).queue();
        }
    }

    private void initTextChannel(Guild guild, String id) {
        TextChannel channel = guild.getTextChannelById(id);

        if (channel != null) {
            // 메시지 삭제 (100개씩 삭제)
            channel.getIterableHistory()
                    .takeAsync(100)
                    .thenAccept(channel::purgeMessages);
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