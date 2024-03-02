package test.model;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import lombok.experimental.FieldNameConstants;

import java.util.List;

/**
 * 试题 实体类。
 * 可以作为试题库中的试题，或者作为答卷中的试题使用(此时可以记录部分题型的答案)
 *
 * @author ysc
 * @since 2023-12-12
 */
@Accessors(chain = true)
@Data
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants
public class Question extends BaseEntity {

    List<Bank> banks;

    private String answerAnalysis;


}
