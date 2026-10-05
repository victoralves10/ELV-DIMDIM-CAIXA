package com.dimdim.caixa.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "conta")
public class Conta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O número da conta é obrigatório.")
    @Pattern(regexp = "\\d{4,10}", message = "O número da conta deve ter de 4 a 10 dígitos.")
    private String numero;

    @NotBlank(message = "A agência é obrigatória.")
    @Pattern(regexp = "\\d{4}", message = "A agência deve ter 4 dígitos.")
    private String agencia = "0001";

    @NotNull(message = "Selecione o tipo da conta.")
    @Enumerated(EnumType.STRING)
    private TipoConta tipo;

    @NotNull(message = "Informe o saldo inicial.")
    @DecimalMin(value = "0.00", message = "O saldo não pode ser negativo.")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal saldo = BigDecimal.ZERO;

    @NotBlank(message = "O PIN é obrigatório.")
    @Pattern(regexp = "\\d{4}", message = "O PIN deve ter 4 dígitos.")
    private String pin;

    @Column(name = "data_abertura", updatable = false)
    private LocalDateTime dataAbertura;

    // N contas pertencem a 1 cliente
    @NotNull(message = "Selecione o cliente titular.")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @PrePersist
    protected void onCreate() {
        this.dataAbertura = LocalDateTime.now();
    }
}
