package com.byteelement.stockguard.service;

import com.byteelement.stockguard.dto.GuardarAlmacenRequest;
import com.byteelement.stockguard.entity.Almacen;
import com.byteelement.stockguard.exception.ConflictoCatalogoException;
import com.byteelement.stockguard.repository.AlmacenRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class AlmacenService {
    private final AlmacenRepository repository;
    public AlmacenService(AlmacenRepository repository) { this.repository = repository; }
    public List<Almacen> listar() { return repository.findAll(Sort.by("id")); }
    public Almacen obtener(Long id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("Almacen no encontrado"));
    }
    @Transactional
    public Almacen crear(GuardarAlmacenRequest request) {
        String codigo = request.codigo().trim().toUpperCase(Locale.ROOT);
        if (repository.existsByCodigo(codigo)) throw new ConflictoCatalogoException("El codigo de almacen ya existe.");
        return repository.saveAndFlush(new Almacen(codigo, request.nombre().trim()));
    }
    @Transactional
    public Almacen actualizar(Long id, GuardarAlmacenRequest request) {
        Almacen almacen = obtener(id);
        String codigo = request.codigo().trim().toUpperCase(Locale.ROOT);
        if (repository.existsByCodigoAndIdNot(codigo, id)) throw new ConflictoCatalogoException("El codigo de almacen ya existe.");
        almacen.actualizar(codigo, request.nombre().trim());
        return repository.saveAndFlush(almacen);
    }
    @Transactional
    public Almacen desactivar(Long id) {
        Almacen almacen = obtener(id);
        almacen.desactivar();
        return repository.saveAndFlush(almacen);
    }
}
