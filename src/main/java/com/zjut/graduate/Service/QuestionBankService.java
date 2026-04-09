package com.zjut.graduate.Service;

import com.zjut.graduate.Po.QuestionBank;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface QuestionBankService {
    int importQuestionsFromCsv(MultipartFile file);

    List<QuestionBank> listAllQuestions();

    boolean deleteQuestion(Long questionId);
}
