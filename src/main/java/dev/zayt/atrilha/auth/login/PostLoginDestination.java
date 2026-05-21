package dev.zayt.atrilha.auth.login;

/**
 * Enumera os destinos possíveis após login bem-sucedido (RF-E1-05).
 *
 * <p>Usado pelos handlers de login para redirecionar o usuário conforme seu
 * papel e estado da conta.</p>
 */
public enum PostLoginDestination {

    TRILHA("/trilha"),
    PAINEL("/painel"),
    VINCULAR("/vincular");

    private final String path;

    PostLoginDestination(String path) {
        this.path = path;
    }

    /** Retorna o caminho de redirecionamento, sempre iniciado com {@code /}. */
    public String path() {
        return this.path;
    }

}
