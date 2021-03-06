package osu.xinyuangui.springbootvuejs.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import osu.xinyuangui.springbootvuejs.domain.SingleFileCode;
import osu.xinyuangui.springbootvuejs.domain.SingleFileCodeBrief;
import osu.xinyuangui.springbootvuejs.repository.SingleFileCodeRepository;
import osu.xinyuangui.springbootvuejs.util.FileIO;
import osu.xinyuangui.springbootvuejs.util.StorageException;
import osu.xinyuangui.springbootvuejs.util.StorageFileNotFoundException;

import java.io.IOException;
import java.util.List;

@Service
public class SingleFileCodeServiceImpl implements SingleFileCodeService {
    private Logger logger = LoggerFactory.getLogger(SingleFileCodeServiceImpl.class);

    @Autowired
    private SingleFileCodeRepository codeRepository;

    @Value("${usercodes.path}")
    private String userCodePath;

    @Value("${executor.url}")
    private String executorUrl;

    @Override
    public List<SingleFileCodeBrief> getSingleFileCodeInfosByPage(Pageable pageable) {
        logger.info("Get Single File Code Infos");
        return codeRepository.findAllInfosByPage(pageable).getContent();
    }

    @Override
    public SingleFileCode getSingleFileCodeById(int id) {
        logger.info("Get Single File Code By Id, id=" + id);
        return codeRepository.getOne(id);
    }

    /**
     * firstly store the code, then save it to the repository
     * @param code
     */
    @Override
    public SingleFileCode updateCode(SingleFileCode code) throws IOException {
        SingleFileCode savedCode = codeRepository.save(code);
        int id = savedCode.getId();
        FileIO.writeCodeToFile(code, userCodePath);
        return savedCode;
    }

    /**
     * call the executor server
     * @param id
     * @param type
     * @param stdin
     * @return
     */
    @Override
    public ResponseEntity runCode(int id, String type, String stdin) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        String input = getRunCodeJson(id, type, stdin);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity entity = new HttpEntity<>(input, headers);
        ResponseEntity response = restTemplate.exchange(executorUrl, HttpMethod.POST, entity, String.class);
        return response;
    }

    private String getRunCodeJson(int id, String type, String stdin) {
        String input = String.format("{\"code_id\":\"%s\",\"type\":\"%s\", \"stdin\":\"%s\"}",
                id, type.toLowerCase(), stdin);
        return input;
    }

    @Override
    public Process getFileReadingProcess(int id, String fileName) throws IOException {
        String destination = String.format("%s/%s/%s", userCodePath, id, fileName);
        return FileIO.readFileProcess(destination);
    }

    @Override
    public String[] getKillFileReadingProcessCommand(int id, String fileName) {
        String destination = String.format("%s/%s/%s", userCodePath, id, fileName);
        return FileIO.killFileReadingProcessCommand(destination);
    }

    @Override
    public int getTotalCount() {
        return (int)codeRepository.count();
    }

    @Override
    public void deleteCode(int id) throws IOException {
        logger.info("delete code: " + id);
        SingleFileCode code = codeRepository.getOne(id);
        codeRepository.delete(code);
        FileIO.deleteCodeFolder(code, userCodePath);
    }

    @Value("${userdirectory.max-size}")
    private long maxDirectorySizeInKb;

    @Override
    public void saveFile(int id, MultipartFile file) throws StorageException {
        String destination = String.format("%s/%s", userCodePath, id);
        FileIO.storeFile(destination, file, maxDirectorySizeInKb * 1024);
    }

    @Override
    public List<String> getFileNames(int id) throws StorageFileNotFoundException {
        String destination = String.format("%s/%s", userCodePath, id);
        return FileIO.getFileList(destination);
    }

    @Override
    public List<String> deleteFiles(int id, List<String> files) throws StorageFileNotFoundException {
        return FileIO.deleteFiles(String.format("%s/%s", userCodePath, id), files);
    }

    @Override
    public Resource getFileResource(int id, String file) throws StorageFileNotFoundException, IOException {
        return FileIO.getResourceOfFile(String.format("%s/%s", userCodePath, id), file);
    }
}
