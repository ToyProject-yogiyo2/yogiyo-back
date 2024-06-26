package toy.yogiyo.document.api;


import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class EnumDocsResponse {

    Map<String, String> days;
    Map<String, String> optionType;
    Map<String, String> visible;
    Map<String, String> adjustmentType;
    Map<String, String> reviewSort;
    Map<String, String> reviewStatus;
}
