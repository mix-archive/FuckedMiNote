package net.micode.notes.model;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatCompletionMessageParam;
import com.openai.models.ChatCompletionSystemMessageParam;
import com.openai.models.ChatCompletionUserMessageParam;
import com.openai.models.ChatModel;

import net.micode.notes.BuildConfig;

import java.util.List;
import java.util.Objects;
import java.util.Optional;


public class IntelligenceIntentService extends IntentService {
    public static final String FORMALIZE_PROMPT = "Assume you are a very experienced and professional assistant. " +
            "Please rewrite the text provided by the user in a more formal tone. " +
            "You are supposed to output using the same language as the user, but in a more formal tone. " +
            "You may not add any information that is not present in the text provided by the user. " +
            "You may not change the meaning of the text provided by the user. " +
            "You may not output anything other than the text provided by the user.";
    public static final int ERROR_CODE = 1;
    public static final int SUCCESS_CODE = 0;
    public static final String COMPLETION_EXTRA = "completion";
    public static final String COMPLETION_QUERY_EXTRA = "query";
    public static final String COMPLETION_PENDING_REPLY_EXTRA = "reply";
    private static final String TAG = IntelligenceIntentService.class.getSimpleName();
    private final OpenAIClient mOpenAIClient;


    public IntelligenceIntentService() {
        super(TAG);
        mOpenAIClient = OpenAIOkHttpClient.builder().apiKey(
                BuildConfig.OPENAI_API_KEY
        ).build();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        assert intent != null;
        PendingIntent reply = Objects.requireNonNull(intent.getParcelableExtra(COMPLETION_PENDING_REPLY_EXTRA));
        try {
            String query = intent.getStringExtra(COMPLETION_QUERY_EXTRA);
            try {
                Optional<String> completion = makeFormalTone(query);
                assert completion.isPresent();
                Intent result = new Intent();
                result.putExtra(COMPLETION_EXTRA, completion.get());
                reply.send(this, SUCCESS_CODE, result);
            } catch (Exception e) {
                Log.e(TAG, "Failed to make formal tone", e);
                reply.send(ERROR_CODE);
            }

        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "Failed to send reply", e);
        }
    }

    private Optional<String> makeFormalTone(String content) {
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .messages(List.of(
                        ChatCompletionMessageParam.ofChatCompletionSystemMessageParam(ChatCompletionSystemMessageParam.builder()
                                .role(ChatCompletionSystemMessageParam.Role.SYSTEM)
                                .content(ChatCompletionSystemMessageParam.Content.ofTextContent(FORMALIZE_PROMPT))
                                .build()
                        ), ChatCompletionMessageParam.ofChatCompletionUserMessageParam(ChatCompletionUserMessageParam.builder()
                                .role(ChatCompletionUserMessageParam.Role.USER)
                                .content(ChatCompletionUserMessageParam.Content.ofTextContent(content))
                                .build())))
                .model(ChatModel.GPT_4O_MINI)
                .build();
        ChatCompletion chatCompletion = mOpenAIClient.chat().completions().create(params).validate();
        ChatCompletion.Choice choice = chatCompletion.choices().get(0);
        if (choice == null) {
            return Optional.empty();
        }
        return choice.message().content();
    }
}