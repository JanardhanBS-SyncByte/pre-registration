package io.mosip.preregistration.application.service;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.kernel.core.authmanager.authadapter.model.AuthUserDetails;
import io.mosip.kernel.core.dataaccess.exception.DataAccessLayerException;
import io.mosip.kernel.core.http.ResponseWrapper;
import io.mosip.kernel.core.idgenerator.spi.PridGenerator;
import io.mosip.kernel.core.idobjectvalidator.spi.IdObjectValidator;
import io.mosip.kernel.core.util.DateUtils;
import io.mosip.preregistration.application.dto.DeletePreRegistartionDTO;
import io.mosip.preregistration.application.dto.DemographicMetadataDTO;
import io.mosip.preregistration.application.dto.DemographicRequestDTO;
import io.mosip.preregistration.application.errorcodes.DemographicErrorCodes;
import io.mosip.preregistration.application.errorcodes.DemographicErrorMessages;
import io.mosip.preregistration.application.exception.BookingDeletionFailedException;
import io.mosip.preregistration.application.exception.RecordFailedToUpdateException;
import io.mosip.preregistration.application.exception.RecordNotFoundException;
import io.mosip.preregistration.application.exception.RecordNotFoundForPreIdsException;
import io.mosip.preregistration.application.repository.DemographicRepository;
import io.mosip.preregistration.application.service.DemographicService;
import io.mosip.preregistration.application.service.DemographicServiceIntf;
import io.mosip.preregistration.application.service.DocumentServiceIntf;
import io.mosip.preregistration.application.service.TransliterationService;
import io.mosip.preregistration.application.service.util.DemographicServiceUtil;
//import io.mosip.preregistration.booking.service.BookingServiceIntf;
import io.mosip.preregistration.core.code.AuditLogVariables;
import io.mosip.preregistration.core.code.StatusCodes;
import io.mosip.preregistration.core.common.dto.AuditRequestDto;
import io.mosip.preregistration.core.common.dto.BookingRegistrationDTO;
import io.mosip.preregistration.core.common.dto.DeleteBookingDTO;
import io.mosip.preregistration.core.common.dto.DemographicResponseDTO;
import io.mosip.preregistration.core.common.dto.DocumentDeleteDTO;
import io.mosip.preregistration.core.common.dto.DocumentDeleteResponseDTO;
import io.mosip.preregistration.core.common.dto.DocumentMultipartResponseDTO;
import io.mosip.preregistration.core.common.dto.ExceptionJSONInfoDTO;
import io.mosip.preregistration.core.common.dto.MainRequestDTO;
import io.mosip.preregistration.core.common.dto.MainResponseDTO;
import io.mosip.preregistration.core.common.dto.PreRegIdsByRegCenterIdDTO;
import io.mosip.preregistration.core.common.dto.PreRegistartionStatusDTO;
import io.mosip.preregistration.core.common.dto.identity.DemographicIdentityRequestDTO;
import io.mosip.preregistration.core.common.dto.identity.Identity;
import io.mosip.preregistration.core.common.dto.identity.IdentityJsonValues;
import io.mosip.preregistration.core.common.entity.ApplicationEntity;
import io.mosip.preregistration.core.common.entity.DemographicEntity;
import io.mosip.preregistration.core.common.entity.DocumentEntity;
import io.mosip.preregistration.core.exception.HashingException;
import io.mosip.preregistration.core.exception.InvalidRequestException;
import io.mosip.preregistration.core.exception.InvalidRequestParameterException;
import io.mosip.preregistration.core.exception.RecordFailedToDeleteException;
import io.mosip.preregistration.core.exception.TableNotAccessibleException;
import io.mosip.preregistration.core.util.AuditLogUtil;
import io.mosip.preregistration.core.util.CryptoUtil;
import io.mosip.preregistration.core.util.HashUtill;
import io.mosip.preregistration.core.util.RequestValidator;
import io.mosip.preregistration.core.util.ValidationUtil;
import io.mosip.preregistration.demographic.dto.DemographicCreateResponseDTO;
import io.mosip.preregistration.demographic.dto.DemographicUpdateResponseDTO;
import io.mosip.preregistration.demographic.dto.DemographicViewDTO;
import io.mosip.preregistration.demographic.exception.system.SystemIllegalArgumentException;

import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.springframework.test.context.ContextConfiguration;
import org.mockito.MockitoAnnotations;
import org.mockito.Mock;
import org.mockito.InjectMocks;

/**
 * Test class to test the PreRegistration Service methods
 * 
 * @since 1.0.0
 * 
 */


@RunWith(JUnit4.class)
@SpringBootTest
@ContextConfiguration(classes = { DemographicServiceIntf.class })
public class DemographicServiceTest {

	/**
	 * Autowired reference for $link{DemographicService}
	 */
	@InjectMocks
	private DemographicServiceIntf preRegistrationService= new DemographicService();

	/**
	 * Mocking the DemographicRepository bean
	 */
	@Mock
	private DemographicRepository demographicRepository;

	/**
	 * Mocking the RestTemplateBuilder bean
	 */
	@Mock(name="restTemplate")
	RestTemplate restTemplate;

	/**
	 * Mocking the PridGenerator bean
	 */
	@Mock
	private PridGenerator<String> pridGenerator;

	/**
	 * Mocking the JsonValidatorImpl bean
	 */
	@Mock(name = "idObjectValidator")
	private IdObjectValidator jsonValidator;

	/**
	 * Autowired reference for $link{DemographicServiceUtil}
	 */
	@Mock
	DemographicServiceUtil serviceUtil;

	@Mock
	ValidationUtil validationUtil;

	JSONParser parser = new JSONParser();

	@Mock
	private DocumentServiceIntf documentServiceIntf;
	//
	//	@MockBean
	//	private BookingServiceIntf bookingServiceIntf;

	@Mock
	private AuditLogUtil auditLogUtil;

	@Mock
	private CryptoUtil cryptoUtil;

	@Mock
	private RequestValidator requestValidator;

	@Mock
	private DemographicEntity entity;

	String userId = "";

	List<DemographicEntity> userEntityDetails = new ArrayList<>();
	List<DemographicViewDTO> responseViewList = new ArrayList<DemographicViewDTO>();
	private DemographicViewDTO preRegistrationViewDTO;
	private DemographicEntity preRegistrationEntity;
	private JSONObject jsonObject;
	private JSONObject jsonTestObject;
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	File fileCr = null;
	File fileUp = null;
	MainRequestDTO<DemographicRequestDTO> request = null;
	DemographicRequestDTO createPreRegistrationDTO = null;
	DemographicResponseDTO demographicResponseDTO = null;
	DemographicCreateResponseDTO demographicResponseForCreateDTO = null;
	DemographicUpdateResponseDTO demographicResponseForUpdateDTO = null;
	DemographicIdentityRequestDTO demographicIdentityRequestDTO = new DemographicIdentityRequestDTO();
	Identity identity = new Identity();
	IdentityJsonValues identityJsonValues = new IdentityJsonValues();
	boolean requestValidatorFlag = false;
	Map<String, String> requestMap = new HashMap<>();
	Map<String, String> requiredRequestMap = new HashMap<>();
	LocalDateTime times = null;
	BookingRegistrationDTO bookingRegistrationDTO;
	MainResponseDTO<DemographicResponseDTO> responseDTO = null;
	MainResponseDTO<DemographicCreateResponseDTO> responseCreateDTO = null;
	AuditRequestDto auditRequestDto = new AuditRequestDto();

	@Value("${version}")
	String versionUrl;

	/**
	 * Reference for ${createId} from property file
	 */
	@Value("${mosip.preregistration.demographic.create.id}")
	private String createId;

	/**
	 * Reference for ${updateId} from property file
	 */
	@Value("${mosip.preregistration.demographic.update.id}")
	private String updateId;

	/**
	 * Reference for ${retrieveId} from property file
	 */
	@Value("${mosip.preregistration.demographic.retrieve.basic.id}")
	private String retrieveId;
	/**
	 * Reference for ${retrieveDetailsId} from property file
	 */
	@Value("${mosip.preregistration.demographic.retrieve.details.id}")
	private String retrieveDetailsId;

	/**
	 * Reference for ${retrieveStatusId} from property file
	 */
	@Value("${mosip.preregistration.demographic.retrieve.status.id}")
	private String retrieveStatusId;

	/**
	 * Reference for ${deleteId} from property file
	 */
	@Value("${mosip.preregistration.demographic.delete.id}")
	private String deleteId;

	/**
	 * Reference for ${updateStatusId} from property file
	 */
	@Value("${mosip.preregistration.demographic.update.status.id}")
	private String updateStatusId;

	@Value("${mosip.pregistration.pagesize}")
	private String pageSize;

	@Value("${preregistartion.config.identityjson}")
	private String preregistrationIdJson;

	/**
	 * Reference for ${dateId} from property file
	 */
	@Value("${mosip.preregistration.demographic.retrieve.date.id}")
	private String dateId;

	LocalDate fromDate = LocalDate.now();
	LocalDate toDate = LocalDate.now();


	private ObjectMapper mapper;

	JSONArray fullname;
	LocalDateTime encryptionDateTime = DateUtils.getUTCCurrentDateTime();
	DemographicServiceIntf spyDemographicService;
	String preId = "";
	String identityMappingJson = "";

	MainResponseDTO<DocumentMultipartResponseDTO> documentResultDto = new MainResponseDTO<>();
	DocumentMultipartResponseDTO documentMultipartResponseDTO = new DocumentMultipartResponseDTO();

	@Mock
	private AuthUserDetails authUserDetails;

	@Mock
	SecurityContextHolder securityContextHolder;

	/**
	 * @throws ParseException
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws org.json.simple.parser.ParseException
	 * @throws URISyntaxException
	 */
	@Before
	public void setup() throws ParseException, FileNotFoundException, IOException,
	org.json.simple.parser.ParseException, URISyntaxException {


		MockitoAnnotations.initMocks(this);
		mapper=new ObjectMapper();
		auditRequestDto = new AuditRequestDto();

		ReflectionTestUtils.setField(preRegistrationService, "jsonValidator", jsonValidator);
		ReflectionTestUtils.setField(preRegistrationService, "pageSize", "1");
		ReflectionTestUtils.setField(preRegistrationService, "preregistrationIdJson", "1");



		preRegistrationEntity = new DemographicEntity();
		ClassLoader classLoader = getClass().getClassLoader();
		URI uri = new URI(
				classLoader.getResource("pre-registration-crby.json").getFile().trim().replaceAll("\\u0020", "%20"));
		fileCr = new File(uri.getPath());
		uri = new URI(
				classLoader.getResource("pre-registration-upby.json").getFile().trim().replaceAll("\\u0020", "%20"));
		fileUp = new File(uri.getPath());

		File file = new File(classLoader.getResource("pre-registration-test.json").getFile());
		jsonObject = (JSONObject) parser.parse(new FileReader(file));

		File fileTest = new File(classLoader.getResource("pre-registration-test.json").getFile());
		jsonTestObject = (JSONObject) parser.parse(new FileReader(fileTest));

		identityMappingJson = "{\r\n" + "	\"identity\": {\r\n" + "		\"name\": {\r\n"
				+ "			\"value\": \"fullName\",\r\n" + "			\"isMandatory\" : true\r\n" + "		},\r\n"
				+ "		\"proofOfAddress\": {\r\n" + "			\"value\" : \"proofOfAddress\"\r\n" + "		},\r\n"
				+ "		\"postalCode\": {\r\n" + "			\"value\" : \"postalCode\"\r\n" + "		}\r\n" + "	}\r\n"
				+ "}  ";

		times = LocalDateTime.now();
		preRegistrationEntity.setCreateDateTime(times);
		preRegistrationEntity.setCreatedBy("9988905444");
		preRegistrationEntity.setStatusCode("Pending_Appointment");
		preRegistrationEntity.setUpdateDateTime(times);
		List<DocumentEntity> documentEntity=new ArrayList<DocumentEntity>();
		DocumentEntity e=new DocumentEntity();
		e.setDocCatCode("POA");
		documentEntity.add(e);
		preRegistrationEntity.setDocumentEntity(documentEntity);
		preRegistrationEntity.setPreRegistrationId("98746563542672");
		preRegistrationEntity
		.setDemogDetailHash(HashUtill.hashUtill(jsonTestObject.toJSONString().getBytes()).toString());
		userEntityDetails.add(preRegistrationEntity);

		logger.info("Entity " + preRegistrationEntity);

		preRegistrationViewDTO = new DemographicViewDTO();
		preRegistrationViewDTO.setStatusCode("Pending_Appointment");
		preRegistrationViewDTO.setPreRegistrationId("98746563542672");
		responseViewList.add(preRegistrationViewDTO);

		createPreRegistrationDTO = new DemographicRequestDTO();
		createPreRegistrationDTO.setDemographicDetails(jsonObject);
		preId = "98746563542672";

		request = new MainRequestDTO<DemographicRequestDTO>();
		request.setId("mosip.pre-registration.demographic.create");
		request.setVersion("1.0");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		mapper.setDateFormat(df);
		mapper.setTimeZone(TimeZone.getDefault());
		request.setRequesttime(df.parse("2019-01-22T07:22:57.186Z"));
		request.setRequesttime(new Timestamp(System.currentTimeMillis()));
		request.setRequest(createPreRegistrationDTO);

		bookingRegistrationDTO = new BookingRegistrationDTO();
		bookingRegistrationDTO.setRegDate("2018-12-10");
		bookingRegistrationDTO.setRegistrationCenterId("1");
		bookingRegistrationDTO.setSlotFromTime("09:00");
		bookingRegistrationDTO.setSlotToTime("09:13");

		requestMap.put("version", versionUrl);

		requiredRequestMap.put("ver", versionUrl);

		responseDTO = new MainResponseDTO<DemographicResponseDTO>();
		responseCreateDTO = new MainResponseDTO<DemographicCreateResponseDTO>();

		responseDTO.setResponsetime(serviceUtil.getCurrentResponseTime());

		responseDTO.setErrors(null);
		responseCreateDTO.setResponsetime(serviceUtil.getCurrentResponseTime());
		responseCreateDTO.setErrors(null);

		auditRequestDto.setActionTimeStamp(LocalDateTime.now(ZoneId.of("UTC")));
		auditRequestDto.setApplicationId(AuditLogVariables.MOSIP_1.toString());
		auditRequestDto.setApplicationName(AuditLogVariables.PREREGISTRATION.toString());
		auditRequestDto.setCreatedBy(AuditLogVariables.SYSTEM.toString());
		auditRequestDto.setHostIp(auditLogUtil.getServerIp());
		auditRequestDto.setHostName(auditLogUtil.getServerName());
		auditRequestDto.setId(AuditLogVariables.NO_ID.toString());
		auditRequestDto.setIdType(AuditLogVariables.PRE_REGISTRATION_ID.toString());
		auditRequestDto.setSessionUserId(AuditLogVariables.SYSTEM.toString());
		auditRequestDto.setSessionUserName(AuditLogVariables.SYSTEM.toString());
		AuthUserDetails applicationUser = Mockito.mock(AuthUserDetails.class);
		Authentication authentication = Mockito.mock(Authentication.class);
		SecurityContext securityContext = Mockito.mock(SecurityContext.class);
		Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);
		Mockito.when(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).thenReturn(applicationUser);
		spyDemographicService = Mockito.spy(preRegistrationService);

		userId = "9988905444";
		identityJsonValues.setIsMandatory(true);
		identityJsonValues.setValue("fullName");
		identity.setName(identityJsonValues);
		identityJsonValues.setIsMandatory(true);
		identityJsonValues.setValue("postalCode");
		//		identity.setPostalCode(identityJsonValues);
		identityJsonValues.setIsMandatory(true);
		identityJsonValues.setValue("proofOfAddress");
		//		identity.setProofOfAddress(identityJsonValues);

		demographicIdentityRequestDTO.setIdentity(identity);

		documentMultipartResponseDTO.setDocCatCode("POA");
		documentMultipartResponseDTO.setDocName("abc.pdf");
		documentMultipartResponseDTO.setDocTypCode("RNC");
		documentMultipartResponseDTO.setLangCode("fra");
		documentMultipartResponseDTO.setDocumentId("a11n3hbr3o30a2");
		documentResultDto.setResponse(documentMultipartResponseDTO);

		Mockito.when(validationUtil.requstParamValidator(Mockito.any())).thenReturn(true);
		Mockito.when(serviceUtil.getJson(Mockito.any())).thenReturn("{\n" + 
				"	\"identity\": {\n" + 
				"		\"name\": {\n" + 
				"			\"value\": \"abcd\"\n" + 
				"		},\n" + 
				"		\"dob\": {\n" + 
				"			\"value\": \"2021\"\n" + 
				"		}\n" + 
				"	},\n" + 
				"	\"documents\": {\n" + 
				"		\"poa\": {\n" + 
				"			\"value\": \"poa\"\n" + 
				"		}\n" + 
				"\n" + 
				"	}\n" + 
				"}");
		preRegistrationService.setup();

		ApplicationEntity applicationEntity=new ApplicationEntity();
		applicationEntity.setAppointmentDate(fromDate);
		applicationEntity.setRegistrationCenterId("1");
		LocalTime fDate = LocalTime.now();
		LocalTime tDate = LocalTime.now();
		applicationEntity.setSlotFromTime(fDate);
		applicationEntity.setSlotToTime(tDate);
		Mockito.when(serviceUtil
				.findApplicationById(Mockito.any())).thenReturn(applicationEntity);

	}

	@Test
	public void getPreRegistrationTest() {
		byte[] encryptedDemographicDetails = jsonTestObject.toJSONString().getBytes();// { 1, 0, 1, 0, 1, 0 };
		preRegistrationEntity.setApplicantDetailJson(encryptedDemographicDetails);
		preRegistrationEntity.setDemogDetailHash(HashUtill.hashUtill(preRegistrationEntity.getApplicantDetailJson()));
		demographicResponseDTO = new DemographicResponseDTO();
		demographicResponseDTO.setPreRegistrationId("98746563542672");
		Mockito.when(serviceUtil.setterForCreateDTO(Mockito.any())).thenReturn(demographicResponseDTO);
		Mockito.when(demographicRepository.findBypreRegistrationId("98746563542672")).thenReturn(preRegistrationEntity);
		Mockito.when(cryptoUtil.decrypt(Mockito.any(), Mockito.any())).thenReturn(jsonObject.toString().getBytes());
		MainResponseDTO<DemographicResponseDTO> res = preRegistrationService.getDemographicData("98746563542672",true);
		assertEquals("98746563542672", res.getResponse().getPreRegistrationId());
	}

	@Test(expected = NullPointerException.class)
	public void createByDateFailureTest() throws Exception {
		InvalidRequestParameterException exception = new InvalidRequestParameterException(
				DemographicErrorCodes.PRG_PAM_APP_012.toString(), DemographicErrorMessages.MISSING_REQUEST_PARAMETER.toString(),
				responseCreateDTO);
		jsonObject = (JSONObject) parser.parse(new FileReader(fileCr));
		Mockito.when(jsonValidator.validateIdObject("[gender,firstname]",jsonObject,
				new ArrayList<String>())).thenReturn(true);

		preRegistrationEntity.setCreateDateTime(null);
		preRegistrationEntity.setCreatedBy("");
		preRegistrationEntity.setPreRegistrationId("");
		Mockito.when(demographicRepository.save(preRegistrationEntity)).thenThrow(exception);
		demographicResponseDTO = new DemographicResponseDTO();
		demographicResponseDTO.setDemographicDetails(jsonObject);
		demographicResponseDTO.setPreRegistrationId("");
		demographicResponseDTO.setCreatedBy("9988905444");
		demographicResponseDTO.setCreatedDateTime(serviceUtil.getLocalDateString(times));
		demographicResponseDTO.setStatusCode("Pending_Appointment");
		createPreRegistrationDTO = new DemographicRequestDTO();
		createPreRegistrationDTO.setDemographicDetails(jsonObject);
		createPreRegistrationDTO.setLangCode("fra");
		request.setRequest(createPreRegistrationDTO);
		List<DemographicResponseDTO> listOfCreatePreRegistrationDTO = new ArrayList<>();
		listOfCreatePreRegistrationDTO.add(demographicResponseDTO);
		ResponseWrapper<String> pridRes = new ResponseWrapper<>();
		pridRes.setResponse("98746563542672");
		ResponseEntity<ResponseWrapper<String>> res = new ResponseEntity<>(pridRes,
				HttpStatus.OK);
		Mockito.when(restTemplate.exchange(Mockito.anyString(), Mockito.eq(HttpMethod.GET), Mockito.any(),
				Mockito.eq(new ParameterizedTypeReference<ResponseWrapper<String>>() {
				}))).thenReturn(res);
		Mockito.when(serviceUtil.generateId()).thenReturn("98746563542672");
		responseDTO.setResponse(demographicResponseDTO);
		MainResponseDTO<io.mosip.preregistration.application.dto.DemographicCreateResponseDTO> actualRes = preRegistrationService.addPreRegistration(request);
		assertEquals(actualRes.getResponse().getStatusCode(), responseDTO.getResponse().getStatusCode());

	}

	@Test
	public void callGetAppointmentDetailsRestServiceTest1()
			throws ParseException, org.json.simple.parser.ParseException {
		byte[] encryptedDemographicDetails = jsonTestObject.toJSONString().getBytes();

		Mockito.when(cryptoUtil.encrypt(Mockito.any(), Mockito.any())).thenReturn(encryptedDemographicDetails);

		preRegistrationEntity.setApplicantDetailJson(encryptedDemographicDetails);
		String userId = "9988905444";
		MainResponseDTO<DemographicMetadataDTO> response = new MainResponseDTO<>();
		List<DemographicViewDTO> viewList = new ArrayList<>();
		DemographicViewDTO viewDto = new DemographicViewDTO();

		viewDto = new DemographicViewDTO();
		viewDto.setPreRegistrationId("98746563542672");
		viewDto.setStatusCode(preRegistrationEntity.getStatusCode());
		viewDto.setBookingMetadata(bookingRegistrationDTO);

		viewList.add(viewDto);

		DemographicMetadataDTO demographicMetadataDTO = new DemographicMetadataDTO();
		//		demographicMetadataDTO.setBasicDetails(viewList);
		response.setResponse(demographicMetadataDTO);
		response.setVersion("1.0");
		Page<DemographicEntity> page = new PageImpl<>(userEntityDetails);
		Mockito.when(cryptoUtil.decrypt(Mockito.any(), Mockito.any()))
		.thenReturn(userEntityDetails.get(0).getApplicantDetailJson());
		Mockito.when(demographicRepository.findByCreatedBy(userId, "Consumed")).thenReturn(userEntityDetails);
		Mockito.when(demographicRepository.findByCreatedByOrderByCreateDateTime(Mockito.any(), Mockito.any(),
				Mockito.any())).thenReturn(page);


		MainResponseDTO<BookingRegistrationDTO> dto = new MainResponseDTO<>();
		dto.setErrors(null);
		ResponseEntity<MainResponseDTO<BookingRegistrationDTO>> respEntity = new ResponseEntity<>(dto, HttpStatus.OK);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);

		Mockito.when(restTemplate.exchange(Mockito.anyString(), Mockito.eq(HttpMethod.GET), Mockito.any(),
				Mockito.eq(new ParameterizedTypeReference<MainResponseDTO<BookingRegistrationDTO>>() {
				}))).thenReturn(respEntity);
		MainResponseDTO<DemographicMetadataDTO> actualRes = preRegistrationService.getAllApplicationDetails(userId,
				"0");
		assertEquals(actualRes.getResponse().getTotalRecords(), "1");

	}

	@Test(expected = SystemIllegalArgumentException.class)
	public void getApplicationDetailsIndexTest() {
		String userId = "12345";
		Mockito.when(demographicRepository.findByCreatedBy(Mockito.anyString(), Mockito.anyString()))
		.thenReturn(userEntityDetails);
		preRegistrationService.getAllApplicationDetails(userId, "abc");

	}

	@Test
	public void getApplicationStatusTest() {
		String preId = "98746563542672";
		byte[] encryptedDemographicDetails = jsonTestObject.toJSONString().getBytes();// { 1, 0, 1, 0, 1, 0 };

		// Mockito.when(cryptoUtil.encrypt(Mockito.any(),Mockito.any())).thenReturn(encryptedDemographicDetails);

		preRegistrationEntity.setApplicantDetailJson(encryptedDemographicDetails);
		preRegistrationEntity.setDemogDetailHash(HashUtill.hashUtill(preRegistrationEntity.getApplicantDetailJson()));
		MainResponseDTO<PreRegistartionStatusDTO> response = new MainResponseDTO<>();
		List<PreRegistartionStatusDTO> statusList = new ArrayList<PreRegistartionStatusDTO>();
		PreRegistartionStatusDTO statusDto = new PreRegistartionStatusDTO();
		statusDto.setPreRegistartionId(preId);
		statusDto.setStatusCode("Pending_Appointment");
		// statusList.add(statusDto);
		response.setResponse(statusDto);

		Mockito.when(demographicRepository.findBypreRegistrationId(ArgumentMatchers.any()))
		.thenReturn(preRegistrationEntity);

		MainResponseDTO<PreRegistartionStatusDTO> actualRes = preRegistrationService.getApplicationStatus(preId,
				userId);
		assertEquals(response.getResponse().getStatusCode(), actualRes.getResponse().getStatusCode());

	}


	@Test(expected = HashingException.class)
	public void getApplicationStatusHashingExceptionTest() {
		String preId = "98746563542672";
		byte[] encryptedDemographicDetails = { 1, 0, 1, 0, 1, 0 };

		// Mockito.when(cryptoUtil.encrypt(Mockito.any(),Mockito.any())).thenReturn(encryptedDemographicDetails);

		preRegistrationEntity.setApplicantDetailJson(encryptedDemographicDetails);
		// preRegistrationEntity.setDemogDetailHash(new
		// String(HashUtill.hashUtill(preRegistrationEntity.getApplicantDetailJson())));
		MainResponseDTO<PreRegistartionStatusDTO> response = new MainResponseDTO<>();
		List<PreRegistartionStatusDTO> statusList = new ArrayList<PreRegistartionStatusDTO>();
		PreRegistartionStatusDTO statusDto = new PreRegistartionStatusDTO();
		statusDto.setPreRegistartionId(preId);
		statusDto.setStatusCode("Pending_Appointment");
		// statusList.add(statusDto);
		response.setResponse(statusDto);

		Mockito.when(demographicRepository.findBypreRegistrationId(ArgumentMatchers.any()))
		.thenReturn(preRegistrationEntity);

		MainResponseDTO<PreRegistartionStatusDTO> actualRes = preRegistrationService.getApplicationStatus(preId,
				userId);
		assertEquals(response.getResponse().getStatusCode(), actualRes.getResponse().getStatusCode());

	}

	@Test(expected = RecordNotFoundException.class)
	public void getApplicationDetailsTransactionFailureCheck() throws Exception {
		String userId = "9988905444";

		Mockito.when(serviceUtil.isNull(Mockito.any())).thenReturn(true);
		//		DataAccessLayerException exception = new DataAccessLayerException(DemographicErrorCodes.PRG_PAM_APP_002.toString(),
		//				DemographicErrorMessages.PRE_REGISTRATION_TABLE_NOT_ACCESSIBLE.toString(), null);
		//		Mockito.when(demographicRepository.findByCreatedBy(Mockito.anyString(), Mockito.anyString()))
		//				.thenThrow(exception);
		preRegistrationService.getAllApplicationDetails(userId, "0");
	}

	@Test
	public void deleteIndividualSuccessTest() throws Exception {
		String preRegId = "98746563542672";
		preRegistrationEntity.setCreateDateTime(times);
		preRegistrationEntity.setCreatedBy("9988905444");
		preRegistrationEntity.setStatusCode("Booked");
		preRegistrationEntity.setUpdateDateTime(times);
		preRegistrationEntity.setApplicantDetailJson(jsonTestObject.toJSONString().getBytes());
		preRegistrationEntity.setPreRegistrationId("98746563542672");

		DocumentDeleteResponseDTO deleteDTO = new DocumentDeleteResponseDTO();
		List<DocumentDeleteResponseDTO> deleteAllList = new ArrayList<>();
		deleteAllList.add(deleteDTO);
		MainResponseDTO<DeleteBookingDTO> delBookingResponseDTO = new MainResponseDTO<>();
		DeleteBookingDTO deleteBookingDTO = new DeleteBookingDTO();
		deleteBookingDTO.setPreRegistrationId("98746563542672");
		List<DeleteBookingDTO> list = new ArrayList<>();
		list.add(deleteBookingDTO);
		delBookingResponseDTO.setResponse(deleteBookingDTO);
		MainResponseDTO<DocumentDeleteResponseDTO> delResponseDto = new MainResponseDTO<>();
		// delResponseDto.setStatus(Boolean.TRUE);
		delResponseDto.setErrors(null);
		delResponseDto.setResponse(deleteDTO);
		delResponseDto.setResponsetime(serviceUtil.getCurrentResponseTime());
		Mockito.when(serviceUtil.isNull(Mockito.any())).thenReturn(false);
		Mockito.when(serviceUtil.checkStatusForDeletion(Mockito.any())).thenReturn(true);
		MainResponseDTO<DeleteBookingDTO>  e=new MainResponseDTO<DeleteBookingDTO>();
		Mockito.when(serviceUtil.deleteBooking(Mockito.any())).thenReturn(e);


		Mockito.when(demographicRepository.findBypreRegistrationId(preRegId)).thenReturn(preRegistrationEntity);

		ResponseEntity<MainResponseDTO<DocumentDeleteResponseDTO>> res = new ResponseEntity<>(delResponseDto,
				HttpStatus.OK);
		ResponseEntity<MainResponseDTO<DeleteBookingDTO>> res1 = new ResponseEntity<>(delBookingResponseDTO,
				HttpStatus.OK);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		Mockito.when(restTemplate.exchange(Mockito.anyString(), Mockito.eq(HttpMethod.DELETE), Mockito.any(),
				Mockito.eq(new ParameterizedTypeReference<MainResponseDTO<DocumentDeleteResponseDTO>>() {
				}))).thenReturn(res);
		Mockito.when(restTemplate.exchange(Mockito.anyString(), Mockito.eq(HttpMethod.DELETE), Mockito.any(),
				Mockito.eq(new ParameterizedTypeReference<MainResponseDTO<DeleteBookingDTO>>() {
				}))).thenReturn(res1);
		Mockito.when(demographicRepository.deleteByPreRegistrationId(preRegistrationEntity.getPreRegistrationId()))
		.thenReturn(1);

		MainResponseDTO<DeletePreRegistartionDTO> actualres = preRegistrationService.deleteIndividual(preRegId, userId);

		assertEquals("9988905444", actualres.getResponse().getDeletedBy());

	}

	@Test(expected = HashingException.class)
	public void getPreRegistrationHashingExceptionTest() {
		byte[] encryptedDemographicDetails = { 1, 0, 1, 0, 1, 0 };

		// Mockito.when(cryptoUtil.encrypt(Mockito.any(),Mockito.any())).thenReturn(encryptedDemographicDetails);

		preRegistrationEntity.setApplicantDetailJson(encryptedDemographicDetails);
		// preRegistrationEntity.setDemogDetailHash(new
		// String(HashUtill.hashUtill(preRegistrationEntity.getApplicantDetailJson())));
		Mockito.when(demographicRepository.findBypreRegistrationId("98746563542672")).thenReturn(preRegistrationEntity);
		Mockito.when(cryptoUtil.decrypt(Mockito.any(), Mockito.any())).thenReturn(jsonObject.toString().getBytes());
		MainResponseDTO<DemographicResponseDTO> res = preRegistrationService.getDemographicData("98746563542672",true);
		assertEquals("98746563542672", res.getResponse().getPreRegistrationId());
	}

	@Test
	public void updatePreRegistrationStatusTest() {
		Mockito.when(demographicRepository.findBypreRegistrationId("98746563542672")).thenReturn(preRegistrationEntity);
		MainResponseDTO<String> res = preRegistrationService.updatePreRegistrationStatus("98746563542672", "Booked",
				userId);
	}

	@Test(expected = RecordNotFoundException.class)
	public void getPreRegistrationFailureTest() {
		Mockito.when(demographicRepository.findBypreRegistrationId("98746563542672")).thenReturn(null);
		preRegistrationService.getDemographicData("98746563542672",true);
	}

	@Test
	public void updatePreRegistrationStatusFailureTest1() {
		Mockito.when(demographicRepository.findBypreRegistrationId("98746563542672")).thenReturn(null);
		MainResponseDTO<String> response = preRegistrationService.updatePreRegistrationStatus("98746563542672", "Booked", userId);
		assertEquals("STATUS_NOT_UPDATED_SUCESSFULLY", response.getResponse());
	}

	@Test
	public void updatePreRegistrationStatusFailureTest2() {
		Mockito.when(demographicRepository.findBypreRegistrationId("98746563542672")).thenReturn(preRegistrationEntity);
		MainResponseDTO<String> response =preRegistrationService.updatePreRegistrationStatus("98746563542672", "NA", userId);
		assertEquals("STATUS_NOT_UPDATED_SUCESSFULLY", response.getResponse());

	}

	@Test(expected = RecordNotFoundForPreIdsException.class)
	public void invalidPreidFailureTest() {
		List<String> preIds = new ArrayList<>();
		preIds.add("");
		PreRegIdsByRegCenterIdDTO preRegIdsByRegCenterIdDTO = new PreRegIdsByRegCenterIdDTO();
		preRegIdsByRegCenterIdDTO.setPreRegistrationIds(null);
		List<String> statusCodes = new ArrayList<>();
		statusCodes.add(StatusCodes.BOOKED.getCode());
		statusCodes.add(StatusCodes.EXPIRED.getCode());
		Mockito.when(demographicRepository.findByStatusCodeInAndPreRegistrationIdIn(statusCodes, preIds))
		.thenReturn(userEntityDetails);
		preRegistrationService.getUpdatedDateTimeForPreIds(preRegIdsByRegCenterIdDTO);

	}

	@Test(expected = RecordNotFoundForPreIdsException.class)
	public void recordeNotFoundTest() {
		List<String> preIds = new ArrayList<>();
		preIds.add("userEntityDetails");
		PreRegIdsByRegCenterIdDTO preRegIdsByRegCenterIdDTO = new PreRegIdsByRegCenterIdDTO();
		preRegIdsByRegCenterIdDTO.setPreRegistrationIds(preIds);
		userEntityDetails = null;
		List<String> statusCodes = new ArrayList<>();
		statusCodes.add(StatusCodes.BOOKED.getCode());
		statusCodes.add(StatusCodes.EXPIRED.getCode());
		Mockito.when(demographicRepository.findByStatusCodeInAndPreRegistrationIdIn(statusCodes, preIds))
		.thenReturn(userEntityDetails);
		preRegistrationService.getUpdatedDateTimeForPreIds(preRegIdsByRegCenterIdDTO);

	}

	@Test(expected = RecordNotFoundForPreIdsException.class)
	public void recordeNotForPreIdFoundTest() {
		List<String> preIds = new ArrayList<>();
		preIds.add("userEntityDetails");
		PreRegIdsByRegCenterIdDTO preRegIdsByRegCenterIdDTO = new PreRegIdsByRegCenterIdDTO();
		preRegIdsByRegCenterIdDTO.setPreRegistrationIds(null);
		userEntityDetails = null;
		List<String> statusCodes = new ArrayList<>();
		statusCodes.add(StatusCodes.BOOKED.getCode());
		statusCodes.add(StatusCodes.EXPIRED.getCode());
		Mockito.when(demographicRepository.findByStatusCodeInAndPreRegistrationIdIn(statusCodes, preIds))
		.thenReturn(userEntityDetails);
		preRegistrationService.getUpdatedDateTimeForPreIds(preRegIdsByRegCenterIdDTO);

	}
}