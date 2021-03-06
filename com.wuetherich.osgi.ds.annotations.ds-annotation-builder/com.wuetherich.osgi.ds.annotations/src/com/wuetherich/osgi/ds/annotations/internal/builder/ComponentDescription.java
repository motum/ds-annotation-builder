/*******************************************************************************
 * Copyright (c) 2011-2013 Gerd W&uuml;therich (gerd@gerd-wuetherich.de).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Gerd W&uuml;therich (gerd@gerd-wuetherich.de) - initial API and implementation
 ******************************************************************************/
package com.wuetherich.osgi.ds.annotations.internal.builder;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import com.wuetherich.osgi.ds.annotations.Constants;
import com.wuetherich.osgi.ds.annotations.internal.DsAnnotationException;
import com.wuetherich.osgi.ds.annotations.internal.DsAnnotationProblem;
import com.wuetherich.osgi.ds.annotations.xml.ObjectFactory;
import com.wuetherich.osgi.ds.annotations.xml.Tcomponent;
import com.wuetherich.osgi.ds.annotations.xml.TconfigurationPolicy;
import com.wuetherich.osgi.ds.annotations.xml.Timplementation;
import com.wuetherich.osgi.ds.annotations.xml.TjavaTypes;
import com.wuetherich.osgi.ds.annotations.xml.Tpolicy;
import com.wuetherich.osgi.ds.annotations.xml.TpolicyOption;
import com.wuetherich.osgi.ds.annotations.xml.Tproperties;
import com.wuetherich.osgi.ds.annotations.xml.Tproperty;
import com.wuetherich.osgi.ds.annotations.xml.Tprovide;
import com.wuetherich.osgi.ds.annotations.xml.Treference;
import com.wuetherich.osgi.ds.annotations.xml.Tservice;

/**
 * <p>
 * </p>
 * 
 * @author Gerd W&uuml;therich (gerd@gerd-wuetherich.de)
 */
public class ComponentDescription {

  /** - */
  private static final String       FIELD_NAME_TARGET                 = "target";

  /** - */
  private static final String       FIELD_NAME_SERVICE                = "service";

  /** - */
  private static final String       MSG_NO_SUPERTYPE_S                = "NO SUPERTYPE '%s'.";

  /** - */
  private static final String       MSG_INVALID_FILTER_S              = "Invalid filter '%s'.";

  /** */
  private static final String       MSG_NON_EXISTING_UNBIND_METHOD_S  = "Non existing unbind method '%s'.";

  /** */
  private static final String       MSG_NON_EXISTING_UPDATED_METHOD_S = "Non existing updated method '%s'.";

  /** - */
  private Tcomponent                _tcomponent;

  /** - */
  private List<DsAnnotationProblem> _problems;

  /** - */
  private TypeDeclaration           _typeDeclaration;

  /** - */
  private String                    _sourceFile;

  /**
   * <p>
   * Creates a new instance of type {@link ComponentDescription}.
   * </p>
   * 
   * @param typeDeclaration
   */
  public ComponentDescription(TypeDeclaration typeDeclaration) {
    Assert.isNotNull(typeDeclaration);

    //
    try {
      CompilationUnit compilationUnit = (CompilationUnit) typeDeclaration.getParent();
      _sourceFile = compilationUnit.getTypeRoot().getCorrespondingResource().getProjectRelativePath()
          .toPortableString();
    } catch (JavaModelException e) {
      //
      // TODO: LOG
    }

    this._typeDeclaration = typeDeclaration;
    _tcomponent = new Tcomponent();
    _problems = new LinkedList<DsAnnotationProblem>();
  }

  /**
   * <p>
   * </p>
   * 
   * @return
   */
  public boolean hasProblems() {
    return !_problems.isEmpty();
  }

  /**
   * <p>
   * </p>
   * 
   * @return
   */
  public List<DsAnnotationProblem> getProblems() {
    return _problems;
  }

  public void setName(String value) {
    _tcomponent.setName(value);
  }

  public void setModifiedMethod(String methodName) {

    // TODO: check exists

    _tcomponent.setModified(methodName);
  }

  public void setDeactivateMethod(String methodName) {

    //
    if (_tcomponent.getActivate() != null && _tcomponent.getActivate().equals(methodName)) {
      throw new DsAnnotationException(String.format("Activate and deactivate method have the same name '%s'.",
          methodName));
    }

    //
    _tcomponent.setDeactivate(methodName);
  }

  public void setActivateMethod(String methodName) {

    //
    if (_tcomponent.getDeactivate() != null && _tcomponent.getDeactivate().equals(methodName)) {
      throw new DsAnnotationException(String.format("Activate and deactivate method have the same name '%s'.",
          methodName));
    }

    //
    _tcomponent.setActivate(methodName);
  }

  public void setEnabled(Boolean value) {
    _tcomponent.setEnabled(value);
  }

  public void setImmediate(Boolean value) {
    _tcomponent.setImmediate(value);

  }

  public void setFactory(String value) {
    _tcomponent.setFactory(value);
  }

  public void addProperties(String value) {
    Tproperties tproperties = new Tproperties();
    _tcomponent.getPropertyOrProperties().add(tproperties);
    tproperties.setEntry(value);
  }

  /**
   * <p>
   * </p>
   * 
   * @param properties
   */
  public void addProperty(Map<String, List<ComponentProperty>> properties) {

    //
    for (String name : properties.keySet()) {

      Tproperty tproperty = new Tproperty();
      _tcomponent.getPropertyOrProperties().add(tproperty);

      List<ComponentProperty> componentProperties = properties.get(name);

      ComponentProperty componentProperty = componentProperties.get(0);
      tproperty.setPropertyName(componentProperty.getName());
      if (componentProperty.getType() != null) {
        tproperty.setPropertyType(TjavaTypes.fromValue(componentProperty.getType()));
      }

      if (componentProperties.size() == 1) {
        tproperty.setPropertyValue(componentProperty.getValue());
      } else {
        StringBuilder stringBuilder = new StringBuilder(System.getProperty("line.separator"));
        for (ComponentProperty prop : componentProperties) {
          stringBuilder.append(prop.getValue());
          stringBuilder.append(System.getProperty("line.separator"));
        }
        tproperty.setValue(stringBuilder.toString());
      }
    }
  }

  /**
   * <p>
   * </p>
   * 
   * @param services
   */
  public void setService(String[] services) {

    if (services == null || services.length == 0) {

      _tcomponent.setService(null);

    } else {

      //
      if (_tcomponent.getService() == null) {
        _tcomponent.setService(new Tservice());
      }

      //
      _tcomponent.getService().getProvide().clear();

      //
      for (String service : services) {

        Assert.isNotNull(service);
        if (!isInstanceOf(service)) {
          throw new DsAnnotationException(String.format(MSG_NO_SUPERTYPE_S, service, FIELD_NAME_SERVICE));
        }

        Tprovide tprovide = new Tprovide();
        tprovide.setInterface(service);
        _tcomponent.getService().getProvide().add(tprovide);
      }
    }
  }

  /**
   * <p>
   * </p>
   * 
   * @param service
   *          the fully qualified name of the service to bind to this reference.
   * @param bind
   *          the name of the bind method
   * @param name
   *          the name of this reference
   * @param cardinality
   *          the cardinality of the reference
   * @param policy
   *          the policy for the reference
   * @param policyOption
   *          TODO
   * @param unbind
   *          the name of the unbind method
   * @param updated
   *          TODO
   * @param target
   *          the target filter for the reference
   */
  public void addReference(String service, String bind, String name, String cardinality, String policy,
      String policyOption, String unbind, String updated, String target) {

    Assert.isNotNull(service);
    Assert.isNotNull(bind);

    // create the reference
    Treference reference = new Treference();

    // step 1: set the interface
    reference.setInterface(service);

    // step 2: set the bind method name
    reference.setBind(bind);

    // step 3: set the name of the bind method
    if (isNotEmpty(name)) {
      if (name == null || name.isEmpty()) {
        throw new DsAnnotationException(String.format("Invalid reference name '%s'.", reference.getName()));
      }
      reference.setName(name);
    } else {
      name = bind;
      if (name.startsWith("add")) {
        name = name.substring("add".length());
      } else if (name.startsWith("set")) {
        name = name.substring("set".length());
      } else if (name.startsWith("bind")) {
        name = name.substring("bind".length());
      }
      if (name == null || name.isEmpty()) {
        throw new DsAnnotationException(String.format(
            "Invalid reference name '%s' (derived from bind method name '%s').", reference.getName(), bind));
      }
      reference.setName(name);
    }

    // [https://github.com/wuetherich/ds-annotation-builder/issues/21]
    // check if reference name is unique
    for (Treference treference : _tcomponent.getReference()) {
      if (treference.getName().equals(reference.getName())) {
        throw new DsAnnotationException(String.format("Reference name '%s' is not unique.", reference.getName()));
      }
    }

    // step 4: set the name of the unbind method
    if (isNotEmpty(unbind)) {
      if ("-".equals(unbind)) {
        reference.setUnbind(null);
      } else {

        //
        if (!checkMethodExists(unbind)) {
          throw new DsAnnotationException(String.format(MSG_NON_EXISTING_UNBIND_METHOD_S, unbind));
        }
        assertNoDsAnnotation(unbind);

        //
        reference.setUnbind(unbind);
      }
    } else {

      //
      String computedUnbindMethodName = computeUnbindMethodName(bind);

      // osgi.cmpn-5.0.0.pdf, 112.13.7.6, p. 322
      // The unbind method is only set if the component type contains a method with the derived name.
      if (checkMethodExists(computedUnbindMethodName)) {
        assertNoDsAnnotation(computedUnbindMethodName);
        reference.setUnbind(computedUnbindMethodName);
      }
    }

    // step 5: set the name of the updated method
    if (isNotEmpty(updated)) {
      if ("-".equals(updated)) {
        reference.setUpdated(null);
      } else {
        //
        if (!checkMethodExists(updated)) {
          throw new DsAnnotationException(String.format(MSG_NON_EXISTING_UPDATED_METHOD_S, updated));
        }
        reference.setUpdated(updated);
      }
    } else {

      //
      String computedUpdatedMethodName = computeUpdatedMethodName(bind);

      // osgi.cmpn-5.0.0.pdf, 112.13.7.8, p. 322
      // The updated method is only set if the component type contains a method with the derived name.
      if (checkMethodExists(computedUpdatedMethodName)) {
        reference.setUpdated(computedUpdatedMethodName);
      }
    }

    // step 6: set the filter
    if (isNotEmpty(target)) {
      try {
        FrameworkUtil.createFilter(target);
        reference.setTarget(target);
      } catch (InvalidSyntaxException e) {
        throw new DsAnnotationException(String.format(MSG_INVALID_FILTER_S, target), FIELD_NAME_TARGET);
      }
    }

    if (isNotEmpty(cardinality)) {
      if ("at_least_one".equalsIgnoreCase(cardinality)) {
        reference.setCardinality("1..n");
      } else if ("optional".equalsIgnoreCase(cardinality)) {
        reference.setCardinality("0..1");
      } else if ("mandatory".equalsIgnoreCase(cardinality)) {
        reference.setCardinality("1..1");
      } else if ("multiple".equalsIgnoreCase(cardinality)) {
        reference.setCardinality("0..n");
      }
    }

    if (isNotEmpty(policy)) {
      reference.setPolicy(Tpolicy.fromValue(policy.toLowerCase()));
    }

    if (isNotEmpty(policyOption)) {
      reference.setPolicyOption(TpolicyOption.fromValue(policyOption.toLowerCase()));
    }

    _tcomponent.getReference().add(reference);
  }

  /**
   * <p>
   * </p>
   * 
   * @param xmlProjectDescription
   * @param outputStream
   */
  public String toXml() {

    try {

      JAXBContext jaxbContext = createJAXBContext();

      // create the marshaller
      Marshaller marshaller = jaxbContext.createMarshaller();

      // set formatted output
      marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      if (_sourceFile != null) {
        try {
          marshaller.setProperty("com.sun.xml.internal.bind.xmlHeaders",
              String.format(Constants.DS_ANNOTATION_BUILDER_GENERATED_COMMENT, _sourceFile));
        } catch (PropertyException ex) {
          marshaller.setProperty("com.sun.xml.bind.xmlHeaders",
              String.format(Constants.DS_ANNOTATION_BUILDER_GENERATED_COMMENT, _sourceFile));
        }
      }
      //
      StringWriter result = new StringWriter();

      //
      marshaller.marshal(new ObjectFactory().createComponent(_tcomponent), result);

      //
      return result.toString();

    } catch (JAXBException e) {
      e.printStackTrace();
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * <p>
   * </p>
   * 
   * @throws JAXBException
   */
  public boolean equals(InputStream inputStream) throws JAXBException {

    JAXBContext jaxbContext = createJAXBContext();
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

    //
    @SuppressWarnings("unchecked")
    JAXBElement<Tcomponent> jaxbElement = (JAXBElement<Tcomponent>) unmarshaller.unmarshal(inputStream);
    Tcomponent tcomponent = jaxbElement.getValue();

    return equals(tcomponent, _tcomponent);
  }

  /**
   * <p>
   * </p>
   * 
   * @param comp1
   * @param comp1
   * @return
   */
  public static boolean equals(Tcomponent comp1, Tcomponent comp2) {

    //
    if (!comp1.equals(comp2)) {
      return false;
    }

    //
    if (comp2.isSetActivate() != comp1.isSetActivate()) {
      return false;
    }

    //
    if (comp2.isSetDeactivate() != comp1.isSetDeactivate()) {
      return false;
    }

    //
    return true;
  }

  /**
   * <p>
   * </p>
   * 
   * @param component
   */
  public void setComponentDefaults() {

    //
    _tcomponent.setName(getImplementationClassName());

    Timplementation timplementation = new Timplementation();
    timplementation.setClazz(getImplementationClassName());
    _tcomponent.setImplementation(timplementation);

    List<String> stypes = getAllDirectlyImplementedSuperInterfaces();

    setService(stypes.toArray(new String[0]));
  }

  /**
   * <p>
   * </p>
   * 
   * @param lowerCase
   */
  public void setConfigurationPolicy(String lowerCase) {

    TconfigurationPolicy tconfigurationPolicy = TconfigurationPolicy.fromValue(lowerCase);

    _tcomponent.setConfigurationPolicy(tconfigurationPolicy);
  }

  public void setServiceFactory(Boolean value) {

    //
    if (_tcomponent.getService() == null) {
      _tcomponent.setService(new Tservice());
    }

    //
    _tcomponent.getService().setServicefactory(value);
  }

  /**
   * <p>
   * </p>
   * 
   * @return
   */
  public String getName() {
    return _tcomponent.getImplementation().getClazz();
  }

  /**
   * <p>
   * </p>
   * 
   * @return
   * @throws JAXBException
   */
  public static JAXBContext createJAXBContext() throws JAXBException {

    // the JAXBContext
    return JAXBContext.newInstance(Tcomponent.class, TconfigurationPolicy.class, Timplementation.class,
        TjavaTypes.class, Tpolicy.class, Tproperties.class, Tproperty.class, Treference.class, Tservice.class);
  }

  /**
   * {@inheritDoc}
   */
  protected String getImplementationClassName() {
    return _typeDeclaration.resolveBinding().getBinaryName();
  }

  /**
   * {@inheritDoc}
   */
  protected List<String> getAllDirectlyImplementedSuperInterfaces() {

    //
    List<String> result = new LinkedList<String>();

    for (Object type : _typeDeclaration.superInterfaceTypes()) {
      result.add(((Type) type).resolveBinding().getBinaryName());
    }

    //
    return result;
  }

  /**
   * {@inheritDoc}
   */
  protected boolean isInstanceOf(String service) {
    return isInstanceOf(service, _typeDeclaration.resolveBinding());
  }

  /**
   * <p>
   * </p>
   * 
   * @param service
   * @param typeBinding
   * @return
   */
  protected boolean isInstanceOf(String service, ITypeBinding typeBinding) {

    //
    if (typeBinding == null) {
      return false;
    }

    //
    if (service.equals(typeBinding.getBinaryName())) {
      return true;
    }

    //
    if (isInstanceOf(service, typeBinding.getSuperclass())) {
      return true;
    }

    //
    for (ITypeBinding iface : typeBinding.getInterfaces()) {
      if (isInstanceOf(service, iface)) {
        return true;
      }
    }

    //
    return false;
  }

  /**
   * <p>
   * </p>
   * 
   * @param bindName
   * @return
   */
  private String computeUnbindMethodName(String bindName) {

    //
    Assert.isNotNull(bindName);

    //
    if (bindName.startsWith("set")) {
      return "unset" + bindName.substring("set".length());
    } else if (bindName.startsWith("add")) {
      return "remove" + bindName.substring("add".length());
    } else {
      return "un" + bindName;
    }
  }

  private String computeUpdatedMethodName(String bindName) {

    //
    Assert.isNotNull(bindName);

    //
    if (bindName.startsWith("set")) {
      return "updated" + bindName.substring("set".length());
    } else if (bindName.startsWith("add")) {
      return "updated" + bindName.substring("add".length());
    } else if (bindName.startsWith("bind")) {
      return "updated" + bindName.substring("bind".length());
    } else {
      return "updated" + bindName;
    }
  }

  /**
   * <p>
   * </p>
   * 
   * @param computedUnbindMethodName
   */
  private boolean checkMethodExists(String computedUnbindMethodName) {

    //
    for (MethodDeclaration methodDeclaration : _typeDeclaration.getMethods()) {
      if (methodDeclaration.getName().getFullyQualifiedName().equals(computedUnbindMethodName)) {
        return true;
      }
    }

    return false;
  }

  /**
   * <p>
   * </p>
   * 
   * @param name
   * @return
   */
  private boolean isNotEmpty(String name) {
    return name != null && name.trim().length() > 0;
  }

  /**
   * <p>
   * </p>
   * 
   * @param methodName
   */
  private void assertNoDsAnnotation(String methodName) {

    //
    for (MethodDeclaration methodDeclaration : _typeDeclaration.getMethods()) {
      if (methodDeclaration.getName().getFullyQualifiedName().equals(methodName)) {

        for (Object modifier : methodDeclaration.modifiers()) {
          if (modifier instanceof MarkerAnnotation) {
            if (DsAnnotationAstVisitor.isDsAnnotation((MarkerAnnotation) modifier)) {
              throw new DsAnnotationException(String.format("Method '%s' must not be annotated with the DS annotation '@%s'.",
                  methodName, ((MarkerAnnotation) modifier).getTypeName()));
            }
          }
        }
      }
    }
  }
}
