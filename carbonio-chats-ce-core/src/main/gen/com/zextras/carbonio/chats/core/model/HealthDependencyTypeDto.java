package com.zextras.carbonio.chats.core.model;

import java.util.Objects;
import java.util.ArrayList;
import io.swagger.annotations.ApiModel;
import com.fasterxml.jackson.annotation.JsonValue;
import javax.validation.constraints.*;
public enum HealthDependencyTypeDto {
    DATABASE, XMPP_SERVER, EVENT_DISPATCHER, STORAGE_SERVICE
}
