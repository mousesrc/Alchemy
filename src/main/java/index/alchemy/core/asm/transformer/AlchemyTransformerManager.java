package index.alchemy.core.asm.transformer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

import index.alchemy.api.IAlchemyClassTransformer;
import index.alchemy.api.annotation.Hook;
import index.alchemy.api.annotation.Patch;
import index.alchemy.api.annotation.Proxy;
import index.alchemy.api.annotation.Unsafe;
import index.alchemy.core.AlchemyEngine;
import index.alchemy.core.debug.AlchemyRuntimeException;
import index.alchemy.util.ASMHelper;
import index.alchemy.util.ReflectionHelper;
import index.alchemy.util.Tool;
import index.project.version.annotation.Omega;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.relauncher.SideOnly;

import static org.objectweb.asm.Opcodes.*;
import static index.alchemy.core.AlchemyConstants.*;

@Omega
public class AlchemyTransformerManager implements IClassTransformer {
	
	public static final String
			I_ALCHEMY_CLASS_TRANSFORMER_DESC = "index/alchemy/api/IAlchemyClassTransformer",
			ALCHEMY_HOOKS_CLASS = "index.alchemy.core.AlchemyHooks",
			ALCHEMY_HOOKS_DESC = "index/alchemy/core/AlchemyHooks",
			HOOK_PROVIDER_ANNOTATION_DESC = "Lindex/alchemy/api/annotation/Hook$Provider;",
			HOOK_ANNOTATION_DESC = "Lindex/alchemy/api/annotation/Hook;",
			HOOK_RESULT_DESC = "index/alchemy/api/annotation/Hook$Result",
			PROXY_PROVIDER_ANNOTATION_DESC = "Lindex/alchemy/api/annotation/Proxy$Provider;",
			PROXY_ANNOTATION_DESC = "Lindex/alchemy/api/annotation/Proxy;",
			PATCH_ANNOTATION_DESC = "Lindex/alchemy/api/annotation/Patch;",
			PATCH_EXCEPTION_ANNOTATION_DESC = "Lindex/alchemy/api/annotation/Patch$Exception;",
			PATCH_SPARE_ANNOTATION_DESC = "Lindex/alchemy/api/annotation/Patch$Spare;",
			PATCH_GENERIC_ANNOTATION_DESC = "Lindex/alchemy/api/annotation/Patch$Generic;",
			FIELD_PROVIDER_ANNOTATION_DESC = "Lindex/alchemy/api/annotation/Field$Provider;",
			FIELD_ANNOTATION_DESC = "Lindex/alchemy/api/annotation/Field;",
			FIELD_ACCESS_DESC = "Lindex/alchemy/api/IFieldAccess;",
			SIDE_ONLY_ANNOTATION_DESC = "Lnet/minecraftforge/fml/relauncher/SideOnly;",
			CALLBACK_FLAG_DESC = "index/alchemy/core/asm/transformer/AlchemyTransformerManager$CALLBACK_FLAG";
	
	public static final Logger logger = LogManager.getLogger(AlchemyTransformerManager.class.getSimpleName());
	
	public static final Set<IClassTransformer> transformers = Sets.newHashSet();
	public static final Map<String, List<IClassTransformer>> transformers_mapping = new HashMap<String, List<IClassTransformer>>() {
		@Override
		public List<IClassTransformer> get(Object key) {
			List<IClassTransformer> result = super.get(key);
			if (result == null)
				put((String) key, result = Lists.newLinkedList());
			return result;
		}
	};
	
	public static final Map<String, List<Runnable>> callback_mapping = new HashMap<String, List<Runnable>>() {
		
		@Override
		public List<Runnable> get(Object key) {
			List<Runnable> result = super.get(key);
			if (result == null)
				put((String) key, result = Lists.newLinkedList());
			return result;
		}
		
	};
	
	@Deprecated
	public static interface CALLBACK_FLAG { }
	
	public static void markClinitCallback(ClassNode node, Runnable... runnables) {
		callback_mapping.get(node.name).addAll(Arrays.asList(runnables));
		for (String i : node.interfaces)
			if (i.equals(CALLBACK_FLAG_DESC))
				return;
		node.interfaces.add(CALLBACK_FLAG_DESC);
		MethodNode clinit = null;
		for (MethodNode method : node.methods)
			if (method.name.equals("<clinit>")) {
				clinit = method;
				break;
			}
		boolean flag = clinit == null;
		if (flag)
			node.methods.add(clinit = new MethodNode(0, "<clinit>", "()V", null, null));
		InsnList list = new InsnList();
		list.add(new LdcInsnNode(node.name));
		list.add(new MethodInsnNode(INVOKESTATIC, "index/alchemy/core/asm/transformer/AlchemyTransformerManager",
				"callback", "(Ljava/lang/String;)V", false));
		if (flag)
			list.add(new InsnNode(RETURN));
		clinit.instructions.insert(list);
	}
	
	public static void callback(String name) {
		List<Runnable> callback = callback_mapping.remove(name);
		if (callback != null)
			callback.forEach(Runnable::run);
	}
	
	static { ReflectionHelper.setClassLoader(ClassWriter.class, AlchemyEngine.getLaunchClassLoader()); }
	
	public static void setup() {
		AlchemyEngine.checkInvokePermissions();
		logger.info("Setup: " + AlchemyTransformerManager.class.getName());
		try {
			loadAllProvider();
		} catch (Exception e) { AlchemyRuntimeException.onException(new RuntimeException(e)); }
	}
	
	public static final boolean debug_print = Boolean.getBoolean("index.alchemy.core.asm.classloader.debug");
	
	@Unsafe(Unsafe.ASM_API)
	private static void loadAllProvider() throws Exception {
		ClassPath path = ClassPath.from(AlchemyTransformerManager.class.getClassLoader());
		for (ClassInfo info : path.getAllClasses())
			if (info.getName().startsWith(MOD_PACKAGE)) {
				ClassReader reader = new ClassReader(info.url().openStream());
				ClassNode node = new ClassNode(ASM5);
				reader.accept(node, 0);
				if (checkSideOnly(node)) {
					loadPatch(node);
					loadField(node);
					loadProxy(node);
					loadHook(node);
					loadTransform(node, info);
				}
			}
	}
	
	@Unsafe(Unsafe.ASM_API)
	private static void loadPatch(ClassNode node) throws Exception {
		if (node.visibleAnnotations != null)
			for (AnnotationNode ann : node.visibleAnnotations)
				if (ann.desc.equals(PATCH_ANNOTATION_DESC)) {
					Patch patch = Tool.makeAnnotation(Patch.class, ann.values);
					transformers_mapping.get(patch.value()).add(new TransformerPatch(node));
					break;
				}
	}
	
	@Unsafe(Unsafe.ASM_API)
	private static void loadHook(ClassNode node) throws Exception {
		if (node.visibleAnnotations != null)
			for (AnnotationNode nann : node.visibleAnnotations)
				if (nann.desc.equals(HOOK_PROVIDER_ANNOTATION_DESC)) {
					for (MethodNode methodNode : node.methods)
						if (checkSideOnly(methodNode) && methodNode.visibleAnnotations != null)
							for (AnnotationNode ann : methodNode.visibleAnnotations)
								if (ann.desc.equals(HOOK_ANNOTATION_DESC)) {
									Hook hook = Tool.makeAnnotation(Hook.class, ann.values,
											"isStatic", false, "type", Hook.Type.HEAD, "disable", "");
									if (hook.disable().isEmpty() || !Boolean.getBoolean(hook.disable())) {
										String args[] = hook.value().split("#");
										if (args.length == 2)
											transformers_mapping.get(args[0]).add(new TransformerHook(methodNode, node.name,
													args[0], args[1], hook.isStatic(), hook.type()));
										else
											AlchemyRuntimeException.onException(new RuntimeException("@Hook method -> split(\"#\") != 2"));
									}
								}
					break;
				}
	}
	
	@Unsafe(Unsafe.ASM_API)
	private static void loadProxy(ClassNode node) throws Exception {
		if (node.visibleAnnotations != null)
			for (AnnotationNode nann : node.visibleAnnotations)
				if (nann.desc.equals(PROXY_PROVIDER_ANNOTATION_DESC)) {
					for (MethodNode methodNode : node.methods)
						if (checkSideOnly(methodNode) && methodNode.visibleAnnotations != null)
							for (AnnotationNode ann : methodNode.visibleAnnotations)
								if (ann.desc.equals(PROXY_ANNOTATION_DESC)) {
									Proxy proxy = Tool.makeAnnotation(Proxy.class, ann.values);
									String args[] = proxy.target().split("#");
									if (args.length == 2)
										transformers_mapping.get(ASMHelper.getClassSrcName(node.name)).add(new TransformerProxy(
												methodNode, proxy.opcode(), args[0], args[1]));
									else
										AlchemyRuntimeException.onException(new RuntimeException("@Hook method -> split(\"#\") != 2"));
								}
					break;
				}
	}
	
	@Unsafe(Unsafe.ASM_API)
	private static void loadField(ClassNode node) throws Exception {
		if (node.visibleAnnotations != null)
			for (AnnotationNode nann : node.visibleAnnotations)
				if (nann.desc.equals(FIELD_PROVIDER_ANNOTATION_DESC)) {
					for (FieldNode fieldNode : node.fields)
						if (checkSideOnly(fieldNode) && fieldNode.desc.equals(FIELD_ACCESS_DESC)) {
							String generics[] = ASMHelper.getGenericType(ASMHelper.getGeneric(fieldNode.signature));
							if (generics.length > 1)
								transformers_mapping.get(ASMHelper.getClassName(generics[0]).replace("/", "."))
										.add(new TransformerFieldAccess(node.name.replace('/', '.'), fieldNode));
						}
					break;
				}
	}
	
	@Unsafe(Unsafe.ASM_API)
	private static void loadTransform(ClassNode node, ClassInfo info) throws Exception {
		if (checkSideOnly(node) && checkTransformer(node))
			try {
				IAlchemyClassTransformer transformer = (IAlchemyClassTransformer) info.load().newInstance();
				if (!transformer.disable())
					if (transformer.getTransformerClassName() == null)
						transformers.add(transformer);
					else
						transformers_mapping.get(transformer.getTransformerClassName()).add(transformer);
			} catch (Exception e) { AlchemyRuntimeException.onException(e); }
	}
	
	@Override
	@Unsafe(Unsafe.ASM_API)
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (debug_print)
			logger.info("loading: " + transformedName);
		for (IClassTransformer transformer : transformers)
			basicClass = transformer.transform(name, transformedName, basicClass);
		if (transformers_mapping.containsKey(transformedName))
			for (IClassTransformer transformer : transformers_mapping.get(transformedName))
				basicClass = transformer.transform(name, transformedName, basicClass);
		return basicClass;
	}
	
	public static boolean checkTransformer(ClassNode node) {
		for (String i : node.interfaces)
			if (i.equals(I_ALCHEMY_CLASS_TRANSFORMER_DESC))
				return true;
		return false;
	}
	
	public static boolean checkSideOnly(ClassNode node) {
		if (node.visibleAnnotations == null)
			return true;
		for (AnnotationNode annotation : node.visibleAnnotations)
			if (annotation.desc.equals(SIDE_ONLY_ANNOTATION_DESC))
				return Tool.makeAnnotation(SideOnly.class, annotation.values).value() == AlchemyEngine.runtimeSide();
		return true;
	}
	
	public static boolean checkSideOnly(MethodNode node) {
		if (node.visibleAnnotations == null)
			return true;
		for (AnnotationNode annotation : node.visibleAnnotations)
			if (annotation.desc.equals(SIDE_ONLY_ANNOTATION_DESC))
				return Tool.makeAnnotation(SideOnly.class, annotation.values).value() == AlchemyEngine.runtimeSide();
		return true;
	}
	
	public static boolean checkSideOnly(FieldNode node) {
		if (node.visibleAnnotations == null)
			return true;
		for (AnnotationNode annotation : node.visibleAnnotations)
			if (annotation.desc.equals(SIDE_ONLY_ANNOTATION_DESC))
				return Tool.makeAnnotation(SideOnly.class, annotation.values).value() == AlchemyEngine.runtimeSide();
		return true;
	}
	
	public static void loadAllTransformClass() {
		transformers_mapping.keySet().forEach(Tool::forName);
	}
	
	public static void transform(String name) { logger.info("Transform: " + name); }

}
