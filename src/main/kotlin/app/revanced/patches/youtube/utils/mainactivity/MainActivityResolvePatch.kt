package app.revanced.patches.youtube.utils.mainactivity

import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.extensions.InstructionExtensions.addInstruction
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchException
import app.revanced.patcher.util.proxy.mutableTypes.MutableClass
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.youtube.utils.mainactivity.fingerprints.MainActivityFingerprint
import app.revanced.util.exception
import app.revanced.util.getTargetIndex
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.util.MethodUtil
import kotlin.properties.Delegates

object MainActivityResolvePatch : BytecodePatch(
    setOf(MainActivityFingerprint)
) {
    lateinit var mainActivityMutableClass: MutableClass
    private lateinit var constructorMethod: MutableMethod
    private lateinit var onBackPressedMethod: MutableMethod
    private lateinit var onCreateMethod: MutableMethod
    private var constructorMethodIndex by Delegates.notNull<Int>()
    private var onBackPressedMethodIndex by Delegates.notNull<Int>()

    override fun execute(context: BytecodeContext) {
        val mainActivityResult = MainActivityFingerprint.result
            ?: throw MainActivityFingerprint.exception
        onCreateMethod = mainActivityResult.mutableMethod
        mainActivityMutableClass = mainActivityResult.mutableClass

        /**
         * Set Constructor Method
         */
        constructorMethod =
            mainActivityMutableClass.methods.find { method -> MethodUtil.isConstructor(method) }
                ?: throw PatchException("Could not find constructorMethod")
        constructorMethodIndex = constructorMethod.implementation!!.instructions.size - 1

        /**
         * Set OnBackPressed Method
         */
        onBackPressedMethod =
            mainActivityMutableClass.methods.find { method -> method.name == "onBackPressed" }
                ?: throw PatchException("Could not find onBackPressedMethod")
        onBackPressedMethodIndex = onBackPressedMethod.getTargetIndex(Opcode.RETURN_VOID)
    }

    fun injectConstructorMethodCall(classDescriptor: String, methodDescriptor: String) =
        constructorMethod.injectMethodCall(classDescriptor, methodDescriptor, constructorMethodIndex)

    fun injectOnBackPressedMethodCall(classDescriptor: String, methodDescriptor: String) =
        onBackPressedMethod.injectMethodCall(classDescriptor, methodDescriptor, onBackPressedMethodIndex)

    fun injectOnCreateMethodCall(classDescriptor: String, methodDescriptor: String) =
        onCreateMethod.injectMethodCall(classDescriptor, methodDescriptor, 0)

    private fun MutableMethod.injectMethodCall(
        classDescriptor: String,
        methodDescriptor: String,
        insertIndex: Int
    ) {
        addInstruction(
            insertIndex,
            "invoke-static/range {p0 .. p0}, $classDescriptor->$methodDescriptor(Landroid/app/Activity;)V"
        )
    }
}