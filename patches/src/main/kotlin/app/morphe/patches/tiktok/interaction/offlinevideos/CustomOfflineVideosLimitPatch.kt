package app.morphe.patches.tiktok.interaction.offlinevideos

import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22c
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val CUSTOM_OFFLINE_VIDEO_LIMIT = 480
private const val CUSTOM_OFFLINE_VIDEO_LIMIT_SMALI = "0x1e0"
private const val CUSTOM_OFFLINE_VIDEOS_HELPER =
    "Lapp/morphe/extension/tiktok/offline/CustomOfflineVideosLimitPatch;"

@Suppress("unused")
val customOfflineVideosLimitPatch = bytecodePatch(
    name = "Custom offline videos limit",
    description = "Adds a configurable custom option to TikTok's offline videos menu. (Supports TikTok 44.9.3.)",
    default = true,
) {
    compatibleWith(*AppCompatibilities.tiktok4383())

    execute {
        OfflineModeSheetOptionsFingerprint.method.apply {
            val freezeListIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_STATIC &&
                        getReference<MethodReference>()?.let { reference ->
                            reference.parameterTypes == listOf("[Ljava/lang/Object;") &&
                                    reference.returnType == "Ljava/util/List;"
                        } == true
            }
            val moveResultIndex = indexOfFirstInstructionOrThrow(freezeListIndex + 1) {
                opcode == Opcode.MOVE_RESULT_OBJECT
            }
            val optionsRegister = getInstruction<OneRegisterInstruction>(moveResultIndex).registerA

            addInstructions(
                moveResultIndex + 1,
                """
                    invoke-static {v$optionsRegister}, $CUSTOM_OFFLINE_VIDEOS_HELPER->getOfflineVideoOptions(Ljava/util/List;)Ljava/util/List;
                    move-result-object v$optionsRegister
                """,
            )
        }

        OfflineModeListConstructorFingerprint.method.apply {
            val capFieldWriteIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.IPUT &&
                        getReference<FieldReference>()?.let { field ->
                            field.definingClass.endsWith("/OfflineModeListVM;") &&
                                    field.type == "I"
                        } == true
            }
            val capRegister = getInstruction<Instruction22c>(capFieldWriteIndex).registerA
            val capLiteralIndex = indexOfFirstInstructionReversedOrThrow(capFieldWriteIndex - 1) {
                this is NarrowLiteralInstruction &&
                        this is OneRegisterInstruction &&
                        registerA == capRegister
            }

            replaceInstruction(capLiteralIndex, "const/16 v$capRegister, $CUSTOM_OFFLINE_VIDEO_LIMIT_SMALI")
        }

        OfflineModeOptionConfigFingerprint.method.apply {
            val ownerClass = definingClass
            var searchStartIndex = 0
            repeat(2) {
                val fieldWriteIndex = indexOfFirstInstructionOrThrow(searchStartIndex) {
                    opcode == Opcode.SPUT_OBJECT &&
                            getReference<FieldReference>()?.let { field ->
                                field.definingClass == ownerClass &&
                                        field.type == "Ljava/util/List;"
                            } == true
                }

                val moveResultIndex = indexOfFirstInstructionReversedOrThrow(fieldWriteIndex - 1) {
                    opcode == Opcode.MOVE_RESULT_OBJECT
                }
                val optionsRegister = getInstruction<OneRegisterInstruction>(moveResultIndex).registerA

                addInstructions(
                    moveResultIndex + 1,
                    """
                        invoke-static {v$optionsRegister}, $CUSTOM_OFFLINE_VIDEOS_HELPER->getOfflineVideoOptions(Ljava/util/List;)Ljava/util/List;
                        move-result-object v$optionsRegister
                    """,
                )
                searchStartIndex = fieldWriteIndex + 3
            }
        }

        OfflineModeOptionEnumFingerprint.method.apply {
            val customEnumSizeLiteralIndex = indexOfFirstInstructionOrThrow {
                this is NarrowLiteralInstruction &&
                        this is OneRegisterInstruction &&
                        narrowLiteral == CUSTOM_OFFLINE_VIDEO_LIMIT
            }
            val customEnumConstructorIndex = indexOfFirstInstructionOrThrow(customEnumSizeLiteralIndex + 1) {
                (opcode == Opcode.INVOKE_DIRECT || opcode == Opcode.INVOKE_DIRECT_RANGE) &&
                        getReference<MethodReference>()?.let { reference ->
                            reference.definingClass == "LX/0pIs;" &&
                                    reference.name == "<init>" &&
                                    reference.returnType == "V" &&
                                    reference.parameterTypes.size == 5
                        } == true
            }

            val constructorCall = getInstruction<RegisterRangeInstruction>(customEnumConstructorIndex)
            val countRegister = constructorCall.startRegister + 3
            val minutesRegister = constructorCall.startRegister + 4
            val sizeRegister = constructorCall.startRegister + 5

            addInstructions(
                customEnumConstructorIndex,
                """
                    invoke-static/range {v$countRegister .. v$countRegister}, $CUSTOM_OFFLINE_VIDEOS_HELPER->getCustomOfflineVideoLimitOrOriginal(I)I
                    move-result v$countRegister
                    invoke-static/range {v$minutesRegister .. v$minutesRegister}, $CUSTOM_OFFLINE_VIDEOS_HELPER->getCustomOfflineVideoMinutesOrOriginal(I)I
                    move-result v$minutesRegister
                    invoke-static/range {v$sizeRegister .. v$sizeRegister}, $CUSTOM_OFFLINE_VIDEOS_HELPER->getCustomOfflineVideoSizeMbOrOriginal(I)I
                    move-result v$sizeRegister
                """,
            )
        }
    }
}
