package org.pehlivan.mert.librarymanagementsystem.controller.loan;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pehlivan.mert.librarymanagementsystem.dto.loan.LoanRequestDto;
import org.pehlivan.mert.librarymanagementsystem.dto.loan.LoanResponseDto;
import org.pehlivan.mert.librarymanagementsystem.dto.loan.UserLoanRequestDto;
import org.pehlivan.mert.librarymanagementsystem.model.loan.LoanStatus;
import org.pehlivan.mert.librarymanagementsystem.service.loan.LoanService;
import org.pehlivan.mert.librarymanagementsystem.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LoanService loanService;

    @MockBean
    private UserService userService;

    private LoanRequestDto loanRequestDto;
    private UserLoanRequestDto userLoanRequestDto;
    private LoanResponseDto loanResponseDto;

    @BeforeEach
    void setUp() {
        loanRequestDto = LoanRequestDto.builder()
                .bookId(1L)
                .userId(1L)
                .borrowedDate(LocalDate.now())
                .build();

        userLoanRequestDto = UserLoanRequestDto.builder()
                .bookId(1L)
                .borrowedDate(LocalDate.now())
                .build();

        loanResponseDto = LoanResponseDto.builder()
                .id(1L)
                .bookId(1L)
                .bookTitle("Test Book")
                .userId(1L)
                .userName("testuser")
                .borrowedDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(14))
                .status(LoanStatus.BORROWED)
                .build();

        // Mock UserService for getAuthenticatedUserId
        when(userService.findByEmail(any())).thenReturn(java.util.Optional.of(
            org.pehlivan.mert.librarymanagementsystem.dto.user.UserResponseDto.builder()
                .id(1L)
                .email("test@example.com")
                .username("testuser")
                .build()
        ));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void borrowBook_ShouldReturnCreatedLoan() throws Exception {
        when(loanService.borrowBook(any(LoanRequestDto.class))).thenReturn(loanResponseDto);

        mockMvc.perform(post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loanRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bookId").value(1))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    @WithMockUser(roles = "READER")
    void borrowBookForUser_ShouldReturnCreatedLoan() throws Exception {
        when(loanService.borrowBookForUser(any(UserLoanRequestDto.class), anyLong())).thenReturn(loanResponseDto);

        mockMvc.perform(post("/api/v1/loans/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userLoanRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bookId").value(1))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void returnBook_ShouldReturnUpdatedLoan() throws Exception {
        loanResponseDto.setStatus(LoanStatus.RETURNED);
        when(loanService.returnBook(anyLong())).thenReturn(loanResponseDto);

        mockMvc.perform(put("/api/v1/loans/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("RETURNED"));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void getLoanHistoryByUser_ShouldReturnLoanList() throws Exception {
        List<LoanResponseDto> loans = Arrays.asList(loanResponseDto);
        when(loanService.getLoanHistoryByUser(anyLong())).thenReturn(loans);

        mockMvc.perform(get("/api/v1/loans/history/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].userId").value(1));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void getAllLoanHistory_ShouldReturnLoanList() throws Exception {
        List<LoanResponseDto> loans = Arrays.asList(loanResponseDto);
        when(loanService.getAllLoanHistory()).thenReturn(loans);

        mockMvc.perform(get("/api/v1/loans/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void getLateLoans_ShouldReturnLoanList() throws Exception {
        List<LoanResponseDto> loans = Arrays.asList(loanResponseDto);
        when(loanService.getLateLoans()).thenReturn(loans);

        mockMvc.perform(get("/api/v1/loans/late"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void getLoanById_ShouldReturnLoan() throws Exception {
        when(loanService.getLoanById(anyLong())).thenReturn(loanResponseDto);

        mockMvc.perform(get("/api/v1/loans/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.bookId").value(1));
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void generateLoanReport_ShouldReturnSuccessMessage() throws Exception {
        when(loanService.generateLoanReport()).thenReturn("Report generated successfully");

        mockMvc.perform(get("/api/v1/loans/report"))
                .andExpect(status().isOk())
                .andExpect(content().string("Report generated successfully"));
    }

    @Test
    @WithMockUser(roles = "READER")
    void borrowBook_WithReaderRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/loans")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loanRequestDto)))
                .andExpect(status().isForbidden());
    }
} 