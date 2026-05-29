package com.zjut.graduate.Service;

import com.zjut.graduate.Po.QuestionBank;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** 题库导入与维护 */
public interface QuestionBankService {

    /** 从 CSV 导入题目 */
    int importQuestionsFromCsv(MultipartFile file, Long createdByUserId);

    /** 查询全部题目 */
    List<QuestionBank> listAllQuestions();

    /** 按 id 删除题目 */
    boolean deleteQuestion(Long questionId);
}
