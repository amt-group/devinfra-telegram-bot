package amtgroup.devinfra.telegram.components.notification.command;

import amtgroup.devinfra.telegram.components.notification.command.dto.SendNotificationCommand;
import amtgroup.devinfra.telegram.components.project.query.ProjectQueryService;
import amtgroup.devinfra.telegram.components.project.query.dto.FindTelegramChatIdByProjectKeyQuery;
import amtgroup.devinfra.telegram.components.telegram.bot.TelegramChatId;
import amtgroup.devinfra.telegram.components.telegram.command.TelegramCommandService;
import amtgroup.devinfra.telegram.components.telegram.command.dto.SendTelegramMessageCommand;
import amtgroup.devinfra.telegram.components.template.model.MessageTemplateId;
import amtgroup.devinfra.telegram.components.template.query.MessageTemplateQueryService;
import amtgroup.devinfra.telegram.components.template.query.dto.FormatMessageQuery;
import amtgroup.devinfra.telegram.components.template.query.dto.FormatMessageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Vitaly Ogoltsov
 */
@Service
@Validated
@Slf4j
public class NotificationCommandService {

    private final ProjectQueryService projectQueryService;
    private final MessageTemplateQueryService messageTemplateQueryService;
    private final TelegramCommandService telegramCommandService;


    public NotificationCommandService(ProjectQueryService projectQueryService,
                                      MessageTemplateQueryService messageTemplateQueryService,
                                      TelegramCommandService telegramCommandService) {

        this.projectQueryService = projectQueryService;
        this.messageTemplateQueryService = messageTemplateQueryService;
        this.telegramCommandService = telegramCommandService;
    }


    /**
     * Отправить уведомление по проекту.
     */
    public void handle(@NotNull @Valid SendNotificationCommand command) {
        TelegramChatId telegramChatId = projectQueryService.handle(new FindTelegramChatIdByProjectKeyQuery(
                command.getProjectKey()
        )).getTelegramChatId();
        if (telegramChatId == null) {
            log.trace("[{}]: не установлен идентификатор telegram-канала.", command.getProjectKey());
            return;
        }
        // отформатировать сообщение по шаблону
        Map<String, Object> variables = new HashMap<>();
        variables.put("serviceKey", command.getServiceKey().toString());
        variables.put("projectKey", command.getProjectKey().toString());
        variables.put("eventTypeId", command.getEventTypeId().toString());
        variables.put("event", command.getEvent());
        FormatMessageQueryResult result = messageTemplateQueryService.formatMessage(new FormatMessageQuery(
                new MessageTemplateId("notification/" + command.getServiceKey()),
                variables
        ));
        // шаблонизатор может вернуть пустой текст или null,
        // в случае если шаблон "принял решение" не отправлять сообщение
        if (StringUtils.isNotBlank(result.getText())) {
            telegramCommandService.sendMessage(new SendTelegramMessageCommand(
                    new TelegramChatId(-301249565L), // todo решить как-то задачу подбора идентификатора чата
                    result.getText()
            ));
        }
    }

}