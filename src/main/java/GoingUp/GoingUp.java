package GoingUp;

import GoingUp.Features.Players;
import GoingUp.Features.Stock;
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
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static GoingUp.Consts.*;
import static GoingUp.Features.Stock.*;

@Slf4j
@RequiredArgsConstructor
public class GoingUp extends ListenerAdapter {

    private final HashMap<Member, Players> joinUsers = new HashMap<>();
    public int currentRound = 1;
    public boolean isRoundEnd = false;
    public Phase currentPhase = Phase.READY;
    public String ADMIN_CONSOLE_STATUS_MESSAGE_ID = "";

    static String imagePath = "src/main/resources/";

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.isFromGuild()) return;

        Channel channel = event.getChannel();
        if (channel instanceof TextChannel textChannel) {
            String[] message = event.getMessage().getContentRaw().split(" ");
            if (textChannel.getId().equals(TC_ADMIN_CONSOLE_ID) && message[0].equals("!콘솔")) {
                adminConsoleButtons(textChannel);
            }
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        Guild guild = event.getGuild();

        //라운드 강제변경
        if (event.getModalId().equals("input_round")) {
            event.deferEdit().queue();

            String newRound = event.getValue("round").getAsString();
            TextChannel textChannel = guild.getTextChannelById(event.getChannelId());

            try {
                int round = Integer.parseInt(newRound);

                if (round < 1 || round > 7) {
                    createMsgAndErase(textChannel, ">>> 라운드 강제 변경 실패: 1 ~ 7 이외의 수를 입력하셨습니다.");
                    loggingChannel(guild, "### 위험: 라운드 강제 변경 실패, 1 ~ 7 이외의 수 입력");
                }

                currentRound = Integer.parseInt(newRound);
                currentPhase = Phase.REST;
                isRoundEnd = false;

                for (Players player : joinUsers.values()) {
                    modifyPlayerWallet(guild, player);
                }

                loggingChannel(guild, "라운드 강제 변경, " + newRound + "라운드로 변경");
            } catch (NumberFormatException e) {
                createMsgAndErase(textChannel, ">>> 라운드 강제 변경 실패: 숫자를 입력하세요.");
                loggingChannel(guild, "### 위험: 라운드 강제 변경 실패, 숫자 이외의 값 입력");
            }

        }
    }

    //어드민 콘솔 버튼
    private void adminConsoleButtons(TextChannel textChannel) {
        textChannel.sendMessage("게임 관리 콘솔입니다.").addActionRow(
                Button.secondary("join_player", "플레이어 모집"),
                Button.success("game_start", "게임 시작")
        ).addActionRow(
                Button.primary("start_market", "장오픈"),
                Button.primary("close_market", "주식변동"),
                Button.primary("select_news", "신문기사 선택"),
                Button.primary("rest", "휴식")
        ).addActionRow(
                Button.success("call_player", "전체소집"),
                Button.secondary("manage_round", "라운드 강제 변경")

        ).addActionRow(
                Button.danger("spec_role", "관전권한부여"),
                Button.danger("reset_game", "초기화")
        ).queue();

        //유휴상태가 아닐때만 현재 상태 표시
        if (currentPhase != Phase.READY) {
            displayAdminConsolePhase(textChannel.getGuild());
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        Channel eventChannel = event.getChannel();
        Member member = event.getMember();
        Guild guild = event.getGuild();

        if (eventChannel instanceof TextChannel textChannel) {
            if (textChannel.getParentCategoryId().equals(CATE_WALLET_ID)) {
                //뉴스
                if(buttonId.startsWith("news_")) {
                    Players player = joinUsers.get(member);
                    player.incrementSelection();

                    if(player.getSelectionCount() == 2) {
                        List<ActionRow> disabledRows = disableButtons(event.getMessage().getActionRows());

                        event.editMessage("2개의 기업이 선택되었습니다.")
                                .setComponents(disabledRows)
                                .queue(message -> {
                                    message.deleteOriginal().queueAfter(5, TimeUnit.SECONDS);
                                });
                    } else {
                        event.deferEdit().queue();
                        List<ActionRow> updatedRows = new ArrayList<>();
                        for (ActionRow row : event.getMessage().getActionRows()) {
                            updatedRows.add(ActionRow.of(
                                    row.getButtons().stream()
                                            .map(button -> button.getId().equals(buttonId)
                                                    ? button.asDisabled() : button)
                                            .toList()));
                        }
                        event.getMessage().editMessageComponents(updatedRows).queue();
                    }

                    //뉴스 사진 보내기
                    String targetCompany=buttonId.substring(5);

                    Optional<Stock> stockOptional = Arrays.stream(Stock.values())
                            .filter(stock -> stock.getName().equals(targetCompany))
                            .findFirst();

                    if(stockOptional.isPresent()) {
                        String path = imagePath+"/1.뉴스기사/"+stockOptional.get().getPath()+"/"+currentRound+"회차.png";

                        File imgFile = new File(path);

                        if(imgFile.exists() && imgFile.isFile()) {
                            textChannel.sendFiles(FileUpload.fromData(imgFile)).queue();
                        } else {
                            loggingChannel(guild, "### 오류: "+path+" 파일이 존재하지 않습니다.");
                        }
                    } else {
                        loggingChannel(guild, "### 오류: 회사 이름이 없습니다.");
                    }

                }
            }else if (textChannel.getId().equals(TC_ADMIN_CONSOLE_ID)) {
                adminConsoleButtonInteract(event, buttonId, textChannel);
            } else if (textChannel.getId().equals(TC_SYSTEM_ID)) {

                //플레이어 참가 버튼
                if (buttonId.startsWith("player_")) {
                    event.deferEdit().queue();

                    String playerNumber = buttonId.replace("player_", "");
                    Role spectatorRole = guild.getRoleById(ROLE_SPECTATOR_ID);

                    if (member != null && spectatorRole != null) {
                        //이미 참여신청함
                        if (joinUsers.containsKey(member)) {
                            createMsgAndErase(textChannel, "["+member.getEffectiveName()+"]님은 이미 참가하였습니다.");
                            loggingChannel(guild, "### 위험: [" + member.getEffectiveName() + "] 중복 신청");
                        } else {
                            boolean hasSpectatorRole = member.getRoles().contains(spectatorRole);
                            if (hasSpectatorRole) {
                                createMsgAndErase(textChannel, "관전 역할으로는 참여할 수 없습니다.");
                                loggingChannel(guild, "### 위험: [" + member.getEffectiveName() + "] 관전 상태에서 참가 신청");
                            } else {
                                //String playerNickname = String.format("%02d. %s", playerNumber, member.getEffectiveName());
                                joinUsers.putIfAbsent(member, new Players(member.getEffectiveName()));

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
                                                player.setChannelId(channel.getId()); // channelId에 채널 ID 저장
                                                initPlayerWallet(player, channel);
                                            }
                                        }, error -> {
                                            // 에러 처리
                                            loggingChannel(guild, "### 오류: 개인 지갑 채널을 생성하는 데 실패했습니다.");
                                        });

                                createMsgAndErase(textChannel, "[" + member.getEffectiveName() + "]참가 완료!");
                                loggingChannel(guild, "[" + member.getEffectiveName() + "] 참가 완료");
                            }
                        }
                    }
                }
            }

        }
    }

    private List<ActionRow> disableButtons(List<ActionRow> actionRows) {
        List<ActionRow> disabledRows = new ArrayList<>();

        for (ActionRow row : actionRows) {
            List<Button> disabledButtons = row.getButtons().stream()
                    .map(Button::asDisabled) // 모든 버튼을 비활성화
                    .toList();
            disabledRows.add(ActionRow.of(disabledButtons));
        }

        return disabledRows;
    }

    //개인 채널에 있는 지갑 텍스트 생성
    private void initPlayerWallet(Players player, TextChannel channel) {
        channel.sendMessage(generatePlayerWalletMessage(player)).queue(message -> {
            player.setWallet_TId(message.getId());
            message.pin().queue(null, failure -> {
                loggingChannel(channel.getGuild(), "### 위험: 지갑 핀 고정 실패, [" + player.getName() + "]님의 지갑 텍스트를 고정하지 못했습니다.");
            });
        }, failure -> {
            loggingChannel(channel.getGuild(), "### 오류: [" + player.getName() + "]님의 지갑 텍스트 생성 실패");
        });
    }

    //개인 채널에 있는 지갑 텍스트 현행화
    private void modifyPlayerWallet(Guild guild, Players player) {
        TextChannel playerChannel = guild.getTextChannelById(player.getChannelId());

        if (playerChannel == null) {
            loggingChannel(guild, "### 오류: [" + player.getName() + "]채널 미존재");
            return;
        }

        playerChannel.retrieveMessageById(player.getWallet_TId()).queue(message -> {
            message.editMessage(generatePlayerWalletMessage(player)).queue();
        }, failure -> {
            loggingChannel(guild, "### 오류: [" + player.getName() + "] 님의 지갑 갱신 실패, 새로운 지갑을 생성시도합니다.");
            initPlayerWallet(player, playerChannel);
        });
    }

    //지갑, 자산 연산후 스트링 반환 메소드
    private String generatePlayerWalletMessage(Players player) {
        int targetRound = currentRound - 1;
        if (isRoundEnd) {
            targetRound++;
        }

        // 각 주식별로 플레이어가 보유한 주식 수와 현재 라운드의 주가를 곱한 총 금액 계산
        int totalAmount = getTotalAmount(player, targetRound);

        return "```[" + player.getName() + "] 님의 총 금액은 [" + totalAmount + "]원 입니다.\n" +
                "당신의 잔액은 [" + player.getVal() + "]원 입니다. \n\n" +
                "시안테마파크 - " + player.getStock_park() + "주\n" +
                "돈내놔캐피탈 - " + player.getStock_capital() + "주\n" +
                "막달려자동차 - " + player.getStock_MCar() + "주\n" +
                "두발로여행사 - " + player.getStock_tour() + "주\n" +
                "효심먹거리투어 - " + player.getStock_eat() + "주\n" +
                "신중자동차 - " + player.getStock_Scar() + "주\n" +
                "맡겨봐은행 - " + player.getStock_bank() + "주\n" +
                "다살려제약 - " + player.getStock_pharmacy() + "주\n" +
                "애프터데스상조 - " + player.getStock_death() + "주\n" +
                "잘살아건설 - " + player.getStock_build() + "주```";
    }

    private static int getTotalAmount(Players player, int targetRound) {
        int totalAmount = player.getVal();

        totalAmount += player.getStock_park() * PARK.getVal()[targetRound];
        totalAmount += player.getStock_capital() * CAPITAL.getVal()[targetRound];
        totalAmount += player.getStock_MCar() * MCAR.getVal()[targetRound];
        totalAmount += player.getStock_tour() * TOUR.getVal()[targetRound];
        totalAmount += player.getStock_eat() * EAT.getVal()[targetRound];
        totalAmount += player.getStock_Scar() * SCAR.getVal()[targetRound];
        totalAmount += player.getStock_bank() * BANK.getVal()[targetRound];
        totalAmount += player.getStock_pharmacy() * PHARMACY.getVal()[targetRound];
        totalAmount += player.getStock_death() * DEATH.getVal()[targetRound];
        totalAmount += player.getStock_build() * BUILD.getVal()[targetRound];
        return totalAmount;
    }

    //운영자콘솔 버튼 이벤트
    private void adminConsoleButtonInteract(ButtonInteractionEvent event, String buttonId, TextChannel textChannel) {

        switch (buttonId) {
            case "join_player":
                event.deferEdit().queue();
                joinPlayerButtons(event, textChannel);
                break;
            case "game_start":
                //todo
                break;
            case "start_market":
                //todo
                break;
            case "close_market":
                //todo
                break;
            case "select_news":
                event.deferEdit().queue();
                selectNews(textChannel);
                break;
            case "rest":
                event.deferEdit().queue();
                rest(textChannel);
                break;
            case "call_player":
                event.deferEdit().queue();
                movePlayerToMainChannel(textChannel);
                break;
            case "manage_round":
                manageRound(event);
                break;
            case "spec_role":
                event.deferEdit().queue();
                event.getChannel().sendMessage("주의: 현재 플레이어에게 플레이어 권한이 삭제되고, 관전 권한을 부여합니다.\n" +
                                "게임이 종료되지 않은경우 누르게되면 스포일러 등 문제가 발생합니다.\n\n" +
                                "### 정말 관전 역할을 부여하시겠습니꺄? (10초뒤 이 메세지는 사라집니다.)")
                        .addActionRow(Button.danger("confirm_spec_role", "확인"))
                        .queue(message -> {
                            message.delete().queueAfter(10, TimeUnit.SECONDS);
                        });
                break;
            case "confirm_spec_role":
                event.deferEdit().queue();
                addSpecRole(textChannel);
                break;
            case "reset_game":
                event.deferEdit().queue();
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
                event.deferEdit().queue();
                resetGame(textChannel);
                break;

        }
    }

    private void joinPlayerButtons(ButtonInteractionEvent event, TextChannel textChannel) {
        TextChannel systemChannel = event.getGuild().getTextChannelById(TC_SYSTEM_ID);
        systemChannel.sendMessage("반갑습니다! 플레이어분들, 하단의 플레이어 버튼을 선택해주세요.").addActionRow(
                        Button.primary("player_1", "플레이어1"),
                        Button.primary("player_2", "플레이어2"),
                        Button.primary("player_3", "플레이어3"),
                        Button.primary("player_4", "플레이어4"),
                        Button.primary("player_5", "플레이어5")
                ).addActionRow(
                        Button.primary("player_6", "플레이어6"))
                .queue();

        createMsgAndErase(textChannel, "플레이어 참가 버튼 생성 완료!");
    }

    //뉴스선택 페이즈
    private void selectNews(TextChannel textChannel) {
        Guild guild = textChannel.getGuild();
        List<String> currentCompanys = new ArrayList<>();

        switch (currentRound) {
            case 1:
                currentCompanys = COMPANYS_ROUND1;
                break;
            case 2:
                currentCompanys = COMPANYS_ROUND2;
                break;
            case 3: case 4: case 5: case 6:
                currentCompanys = COMPANYS_ALL;
                break;
            case 7:
                currentCompanys = COMPANYS_ROUND7;
                break;
        }

        for(Players player : joinUsers.values()){
            player.initSelection();
            TextChannel playerChannel = guild.getTextChannelById(player.getChannelId());

            playerChannel.sendMessage("## " + currentRound + "라운드 기사선택\n 신문 기사를 확인할 기업 2개를 선택해주세요.")
                    .addComponents(generateButtons("news_",currentCompanys))
                    .queue();
        }

        currentPhase = Phase.NEWS;
        displayAdminConsolePhase(guild);

        loggingChannel(guild, "페이즈 전환: "+ currentRound + "라운드 기사선택 시작");
    }

    //휴식 페이즈
    private void rest(TextChannel textChannel) {
        Guild guild = textChannel.getGuild();

        long currentTimestamp = Instant.now().getEpochSecond(); // 현재 유닉스 타임스탬프 (초 단위)
        long fiveMinutesLater = currentTimestamp + 300; // 5분 후의 유닉스 타임스탬프

        textChannel.sendMessage(">>> "+currentRound + "라운드 휴식 진행중...\n" +
                "<t:"+ fiveMinutesLater + ":R> 종료").queue(message -> {
                    message.delete().queueAfter(310, TimeUnit.SECONDS);
        });

        TextChannel systemChannel = guild.getTextChannelById(TC_SYSTEM_ID);
        systemChannel.sendMessage(">>> "+currentRound + "라운드 휴식 진행중...\n" +
                "<t:"+ fiveMinutesLater + ":R> 종료").queue(message -> {
            message.delete().queueAfter(310, TimeUnit.SECONDS);
        });

        StringBuilder playerVal = new StringBuilder();
        for(Players player : joinUsers.values()){
            playerVal.append("\n" + player.getName() + ": ")
                    .append(getTotalAmount(player, currentRound));
        }

        loggingChannel(guild, "페이즈 전환: "+ currentRound + "라운드 휴식 시작");
        loggingChannel(guild, "```"+currentRound+"라운드 보유 금액" +
                playerVal+"```");

        currentPhase = Phase.REST;
        currentRound++;
        isRoundEnd = false;
        displayAdminConsolePhase(guild);
    }

    private void movePlayerToMainChannel(TextChannel channel) {
        Guild guild = channel.getGuild();

        // 이동 대상 멤버 수 추적
        int totalMembers = joinUsers.size();
        AtomicInteger completedCount = new AtomicInteger(0);

        channel.sendMessage(">>> 이동 중...").queue(message -> {
            if (joinUsers.keySet().isEmpty()) {
                editMsgAndErase(message, ">>> 이동할 플레이어가 없습니다!");
                loggingChannel(guild, "### 위험: 전체소집 실패, 이동할 플레이어가 없음");
            }

            for (Member member : joinUsers.keySet()) {
                if (member.getVoiceState() == null || member.getVoiceState().getChannel() == null) {
                    guild.retrieveMemberById(member.getId()).queue(refreshedMember -> {
                        handleMemberVoiceChannel(refreshedMember, guild);
                    });
                } else {
                    handleMemberVoiceChannel(member, guild);
                }
                if (totalMembers == completedCount.incrementAndGet()) {
                    editMsgAndErase(message, ">>> 이동 완료!");
                    loggingChannel(guild, "전체 소집 완료");
                }
            }
        });

    }

    private void handleMemberVoiceChannel(Member member, Guild guild) {
        VoiceChannel mainChannel = guild.getVoiceChannelById(VC_MAIN_ID);

        if (member.getVoiceState() == null || member.getVoiceState().getChannel() == null) {
            loggingChannel(guild, "### 위험: [" + member.getEffectiveName() + "]님은 음성채널에 있지 않습니다.");
            return;
        }

        if (mainChannel != null) {
            guild.moveVoiceMember(member, mainChannel).queue(
                    success -> {
                        loggingChannel(guild, "[" + member.getEffectiveName() + "] 이동 성공");
                    },
                    error -> {
                        loggingChannel(guild, "### 오류: 알 수 없는 이유로 [" + member.getEffectiveName() + "]님을 이동시키지 못했습니다.");
                    }
            );
        }
    }

    //라운드 강제 변경
    private void manageRound(ButtonInteractionEvent event) {

        if (currentPhase == Phase.READY) {
            event.deferEdit().queue();
            createMsgAndErase(event.getChannel().asTextChannel(), "게임이 진행중이지 않습니다.");
            loggingChannel(event.getGuild(), "### 위험: 게임 미진행 중 강제 변경 시도");
            return;
        }

        TextInput quantityInput = TextInput.create("round", "변경할 라운드 입력", TextInputStyle.SHORT)
                .setPlaceholder("주의: 해당 라운드의 찌라시 구매 페이즈로 전환됩니다.")
                .setRequired(true)
                .build();
        Modal modal = Modal.create("input_round", "라운드 강제 변경")
                .addActionRow(quantityInput)
                .build();
        event.replyModal(modal).queue();

    }

    //관전 권한 부여
    private void addSpecRole(TextChannel textChannel) {
        Role playerRole = textChannel.getGuild().getRoleById(ROLE_PLAYER_ID);
        Role spectatorRole = textChannel.getGuild().getRoleById(ROLE_SPECTATOR_ID);

        int totalMembers = joinUsers.size();
        AtomicInteger completedCount = new AtomicInteger(0);

        textChannel.sendMessage(">>> 플레이어 권한을 삭제하고 관전 권한 부여중...").queue(message -> {
            if (joinUsers.keySet().isEmpty()) {
                editMsgAndErase(message, ">>> 플레이어가 없습니다!");
                loggingChannel(textChannel.getGuild(), "### 위험: 관전 권한 부여 실패, 플레이어가 없음");
            }
            for (Member member : joinUsers.keySet()) {
                removeRole(member, playerRole);
                assignRole(member, spectatorRole);

                if (totalMembers == completedCount.incrementAndGet()) {
                    editMsgAndErase(message, ">>> 권한 변경 완료!\n제대로 변경되지 않은 경우 한번 더 시도해주세요.");
                    loggingChannel(textChannel.getGuild(), "관전 권한 부여 완료");
                }
            }

        });


    }

    //게임 초기화 로직
    private void resetGame(TextChannel textChannel) {
        currentRound = 1;
        currentPhase = Phase.READY;

        Guild guild = textChannel.getGuild();

        initTextChannel(guild, TC_SYSTEM_ID);

        //개인채널삭제
        for (Players player : joinUsers.values()) {
            TextChannel targetChannel = guild.getTextChannelById(player.getChannelId());

            if (targetChannel != null) {
                targetChannel.delete().queue(
                        success -> {
                            loggingChannel(guild, "[" + player.getName() + "] 채널 삭제 성공");
                        },
                        error -> {
                            loggingChannel(guild, "### 오류: 알 수 없는 이유로 [" + player.getName() + "]의 채널을 삭제하지 못했습니다.");
                        });
            }
        }

        //todo 주가차트 초기화 후 0회차 사진 올리기
        //todo 딜러콘솔3개 초기화후 콘솔 버튼 올리기

        joinUsers.clear();
    }


    //=============================================================================================
    //버튼 생성기
    private List<ActionRow> generateButtons(String type, List<String> companys) {
        List<ActionRow> actionRows = new ArrayList<>();
        List<Button> buttons = new ArrayList<>();

        for (String company : companys) {
            buttons.add(Button.primary(type + company, company));

            if (buttons.size() == 5) {
                actionRows.add(ActionRow.of(buttons));
                buttons.clear();
            }
        }
        // 남은 버튼 처리
        if (!buttons.isEmpty()) {
            actionRows.add(ActionRow.of(buttons));
        }

        return actionRows;
    }

    private void displayAdminConsolePhase(Guild guild){
        TextChannel textChannel = guild.getTextChannelById(TC_ADMIN_CONSOLE_ID);

        if (ADMIN_CONSOLE_STATUS_MESSAGE_ID.isEmpty()) {
            textChannel.sendMessage(">>> 현재 라운드 : " + currentRound + "\n" +
                    "현재 페이즈 : " + currentPhase.getDesc()).queue(message -> ADMIN_CONSOLE_STATUS_MESSAGE_ID = message.getId());
        } else {
            textChannel.retrieveMessageById(ADMIN_CONSOLE_STATUS_MESSAGE_ID).queue(message ->
                    message.editMessage(">>> 현재 라운드 : " + currentRound + "\n" +
                            "현재 페이즈 : " + currentPhase.getDesc()).queue()
            );
        }
    }

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