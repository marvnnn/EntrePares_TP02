package arquivo;

import aed3.*;
import entidades.Curso;
import entidades.CursoUsuario;

import java.util.ArrayList;

public class ArquivoCurso extends Arquivo<Curso> {

    HashExtensivel<ParCodigoId> indiceCodigo;
    ArvoreBMais<ParNomeId> indiceNome;
    ArquivoCursoUsuario arqCursoUsuario;

    public ArquivoCurso() throws Exception {
        super("curso", Curso.class.getConstructor());
        indiceCodigo = new HashExtensivel<>(
                ParCodigoId.class.getConstructor(),
                4,
                "./dados/curso/indiceCodigo.d.db",
                "./dados/curso/indiceCodigo.c.db");
        indiceNome = new ArvoreBMais<>(
                ParNomeId.class.getConstructor(),
                4,
                "./dados/curso/indiceNome.db");
        arqCursoUsuario = new ArquivoCursoUsuario();
    }

    @Override
    public int create(Curso curso) throws Exception {
        int id = super.create(curso);
        indiceCodigo.create(new ParCodigoId(curso.getCodigoCompartilhavel(), id));
        indiceNome.create(new ParNomeId(curso.getNome(), id));
        return id;
    }

    public Curso readCodigo(String codigo) throws Exception {
        ParCodigoId pci = indiceCodigo.read(Math.abs(codigo.hashCode()));
        if (pci == null)
            return null;
        return read(pci.getId());
    }

    public Curso[] readNome(String nome) throws Exception {
        ArrayList<ParNomeId> pnis = indiceNome.read(new ParNomeId(nome, -1));
        if (pnis.isEmpty())
            return new Curso[0];

        Curso[] cursos = new Curso[pnis.size()];
        int i = 0;
        for (ParNomeId pni : pnis) {
            cursos[i++] = super.read(pni.getId());
        }
        return cursos;
    }

    /**
     * Associa um usuário a um curso com um determinado papel.
     * @param idUsuario ID do usuário
     * @param idCurso ID do curso
     * @param papel Papel do usuário no curso (INSTRUTOR, PARTICIPANTE)
     * @return ID da associação criada
     * @throws Exception
     */
    public int associarUsuario(int idUsuario, int idCurso, String papel) throws Exception {
        // Verifica se a associação já existe
        if (arqCursoUsuario.exists(idUsuario, idCurso)) {
            throw new Exception("Usuário já está associado a este curso");
        }

        CursoUsuario assoc = new CursoUsuario(idUsuario, idCurso, papel);
        return arqCursoUsuario.create(assoc);
    }

    /**
     * Remove a associação entre um usuário e um curso.
     * @param idUsuario ID do usuário
     * @param idCurso ID do curso
     * @return true se a associação foi removida, false caso contrário
     * @throws Exception
     */
    public boolean desassociarUsuario(int idUsuario, int idCurso) throws Exception {
        // Primeiro encontramos a associação
        CursoUsuario[] assocs = arqCursoUsuario.readPorUsuario(idUsuario);
        for (CursoUsuario assoc : assocs) {
            if (assoc.getIdCurso() == idCurso) {
                return arqCursoUsuario.delete(assoc.getID());
            }
        }
        return false;
    }

    /**
     * Retorna todos os usuários associados a um curso específico.
     */
    public CursoUsuario[] getUsuariosDoCurso(int idCurso) throws Exception {
        return arqCursoUsuario.readPorCurso(idCurso);
    }

    /**
     * Retorna todos os cursos associados a um usuário específico.
     */
    public CursoUsuario[] getCursosDoUsuario(int idUsuario) throws Exception {
        return arqCursoUsuario.readPorUsuario(idUsuario);
    }

    /**
     * Verifica se um usuário tem um determinado papel em um curso.
     */
    public boolean usuarioTemPapel(int idUsuario, int idCurso, String papel) throws Exception {
        CursoUsuario[] assocs = arqCursoUsuario.readPorUsuario(idUsuario);
        for (CursoUsuario assoc : assocs) {
            if (assoc.getIdCurso() == idCurso && assoc.getPapel().equals(papel)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retorna o ID do instrutor de um curso (usuário com papel INSTRUTOR).
     */
    public Integer getInstrutorDoCurso(int idCurso) throws Exception {
        CursoUsuario[] assocs = arqCursoUsuario.readPorCurso(idCurso);
        for (CursoUsuario assoc : assocs) {
            if (assoc.getPapel().equals(CursoUsuario.INSTRUTOR)) {
                return assoc.getIdUsuario();
            }
        }
        return null; // Nenhum instrutor encontrado
    }

    public Curso[] readAll() throws Exception {
        ArrayList<ParNomeId> pnis = indiceNome.read(null);
        if (pnis.isEmpty())
            return new Curso[0];

        Curso[] cursos = new Curso[pnis.size()];
        int i = 0;
        for (ParNomeId pni : pnis) {
            cursos[i++] = super.read(pni.getId());
        }
        return cursos;
    }

    @Override
    public boolean delete(int id) throws Exception {
        Curso curso = read(id);
        if (curso != null) {
            // Primeiro remove todas as associações relacionadas a este curso
            CursoUsuario[] assocs = arqCursoUsuario.readPorCurso(curso.getID());
            for (CursoUsuario assoc : assocs) {
                arqCursoUsuario.delete(assoc.getID());
            }

            if (super.delete(id)) {
                indiceCodigo.delete(Math.abs(curso.getCodigoCompartilhavel().hashCode()));
                indiceNome.delete(new ParNomeId(curso.getNome(), curso.getID()));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean update(Curso novoCurso) throws Exception {
        Curso curso = read(novoCurso.getID());
        if (curso == null)
            return false;
        if (super.update(novoCurso)) {
            if (!curso.getCodigoCompartilhavel().equals(novoCurso.getCodigoCompartilhavel())) {
                indiceCodigo.delete(Math.abs(curso.getCodigoCompartilhavel().hashCode()));
                indiceCodigo.create(new ParCodigoId(novoCurso.getCodigoCompartilhavel(), novoCurso.getID()));
            }
            if (!curso.getNome().equals(novoCurso.getNome())) {
                indiceNome.delete(new ParNomeId(curso.getNome(), curso.getID()));
                indiceNome.create(new ParNomeId(novoCurso.getNome(), novoCurso.getID()));
            }
            return true;
        }
        return false;
    }

    public void close() throws Exception {
        super.close();
        indiceCodigo.close();
        indiceNome.close();
        arqCursoUsuario.close();
    }
}