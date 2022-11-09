package chat.sphinx.feature_repository.mappers.action_track

import chat.sphinx.conceptcoredb.ActionTrackDbo
import chat.sphinx.feature_repository.mappers.ClassMapper
import chat.sphinx.wrapper_action_track.*
import chat.sphinx.wrapper_action_track.action_wrappers.MessageAction
import chat.sphinx.wrapper_action_track.action_wrappers.toJson
import chat.sphinx.wrapper_action_track.action_wrappers.toMessageActionOrNull
import com.squareup.moshi.Moshi
import io.matthewnelson.concept_coroutines.CoroutineDispatchers


internal class ActionTrackDboMessagePresenterMapper(
    dispatchers: CoroutineDispatchers,
    val moshi: Moshi
): ClassMapper<ActionTrackDbo, MessageAction?>(dispatchers) {

    override suspend fun mapFrom(value: ActionTrackDbo): MessageAction? {
        return value.meta_data.value.toMessageActionOrNull(moshi)
    }

    override suspend fun mapTo(value: MessageAction?): ActionTrackDbo {
        return ActionTrackDbo(
            id = ActionTrackId(Long.MAX_VALUE),
            type = ActionTrackType.Message,
            meta_data = ActionTrackMetaData(value?.toJson(moshi) ?: "{}"),
            uploaded = false.toActionTrackUploaded(),
        )
    }
}