package se.de.hu_berlin.informatik.spectra.provider.tracecobertura.data;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * A message adapter that keeps list of last N events.
 *
 * @author piotr.tabor@gmail.com.
 */
public class HistoryMethodAdapter extends MethodVisitor {

	private final LinkedList<AbstractInsnNode> backlog = new LinkedList<>();
	private final int eventsToTrace;

	public HistoryMethodAdapter(MethodVisitor mv, int eventsToTrace) {
		super(Opcodes.ASM4, mv);
		this.eventsToTrace = eventsToTrace;
	}
	public List<AbstractInsnNode> backlog() {
		return Collections.unmodifiableList(backlog);
	}

	private void appendToBacklog(AbstractInsnNode node) {
		if (backlog.size() >= eventsToTrace) {
			backlog.removeFirst();
		}
		backlog.addLast(node);
	}

	@Override
	public void visitFieldInsn(int arg0, String arg1, String arg2, String arg3) {
		super.visitFieldInsn(arg0, arg1, arg2, arg3);
		appendToBacklog(new FieldInsnNode(arg0, arg1, arg2, arg3));
	}

	@Override
	public void visitFrame(int arg0, int arg1, Object[] arg2, int arg3,
			Object[] arg4) {
		super.visitFrame(arg0, arg1, arg2, arg3, arg4);
		appendToBacklog(new FrameNode(arg0, arg1, arg2, arg3, arg4));
	}

	@Override
	public void visitIincInsn(int arg0, int arg1) {
		super.visitIincInsn(arg0, arg1);
		appendToBacklog(new IincInsnNode(arg0, arg1));
	}

	@Override
	public void visitInsn(int arg0) {
		super.visitInsn(arg0);
		appendToBacklog(new InsnNode(arg0));
	}

	@Override
	public void visitIntInsn(int arg0, int arg1) {
		super.visitIntInsn(arg0, arg1);
		appendToBacklog(new IntInsnNode(arg0, arg1));
	}

	@Override
	public void visitJumpInsn(int arg0, Label arg1) {
		super.visitJumpInsn(arg0, arg1);
		appendToBacklog(new JumpInsnNode(arg0, new LabelNode(arg1)));
	}

	@Override
	public void visitLabel(Label arg0) {
		super.visitLabel(arg0);
		appendToBacklog(new LabelNode(arg0));
	}

	@Override
	public void visitLdcInsn(Object arg0) {
		super.visitLdcInsn(arg0);
		appendToBacklog(new LdcInsnNode(arg0));
	}

	@Override
	public void visitLineNumber(int arg0, Label arg1) {
		super.visitLineNumber(arg0, arg1);
		appendToBacklog(new LineNumberNode(arg0, new LabelNode(arg1)));
	}

	@Override
	public void visitLookupSwitchInsn(Label arg0, int[] arg1, Label[] arg2) {
		super.visitLookupSwitchInsn(arg0, arg1, arg2);
		LabelNode[] nodes = new LabelNode[arg2.length];
		for (int i = 0; i < arg2.length; i++) {
			nodes[i] = new LabelNode(arg2[i]);
		}
		appendToBacklog(new LookupSwitchInsnNode(new LabelNode(arg0), arg1,
				nodes));
	}

	@SuppressWarnings("deprecation")
	@Override
	public void visitMethodInsn(int arg0, String arg1, String arg2, String arg3) {
		super.visitMethodInsn(arg0, arg1, arg2, arg3);
		appendToBacklog(new MethodInsnNode(arg0, arg1, arg2, arg3));
	}

	@Override
	public void visitMultiANewArrayInsn(String arg0, int arg1) {
		super.visitMultiANewArrayInsn(arg0, arg1);
		appendToBacklog(new MultiANewArrayInsnNode(arg0, arg1));
	}

	@Override
	public void visitTableSwitchInsn(int arg0, int arg1, Label arg2,
			Label... arg3) {
		super.visitTableSwitchInsn(arg0, arg1, arg2, arg3);
		LabelNode[] nodes = new LabelNode[arg3.length];
		for (int i = 0; i < arg3.length; i++) {
			nodes[i] = new LabelNode(arg3[i]);
		}
		appendToBacklog(new TableSwitchInsnNode(arg0, arg1,
				new LabelNode(arg2), nodes));
	}

	@Override
	public void visitTypeInsn(int arg0, String arg1) {
		super.visitTypeInsn(arg0, arg1);
		appendToBacklog(new TypeInsnNode(arg0, arg1));
	}

	@Override
	public void visitVarInsn(int arg0, int arg1) {
		super.visitVarInsn(arg0, arg1);
		appendToBacklog(new VarInsnNode(arg0, arg1));
	}
}
