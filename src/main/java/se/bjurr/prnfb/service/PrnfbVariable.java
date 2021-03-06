package se.bjurr.prnfb.service;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Ordering.usingToString;
import static java.util.regex.Pattern.compile;
import static se.bjurr.prnfb.http.UrlInvoker.urlInvoker;
import static se.bjurr.prnfb.http.UrlInvoker.HTTP_METHOD.GET;
import static se.bjurr.prnfb.service.RepoProtocol.http;
import static se.bjurr.prnfb.service.RepoProtocol.ssh;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import se.bjurr.prnfb.http.ClientKeyStore;
import se.bjurr.prnfb.http.Invoker;
import se.bjurr.prnfb.http.UrlInvoker;
import se.bjurr.prnfb.listener.PrnfbPullRequestAction;
import se.bjurr.prnfb.settings.PrnfbNotification;

import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestParticipant;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.repository.RepositoryCloneLinksRequest;
import com.atlassian.bitbucket.repository.RepositoryService;
import com.atlassian.bitbucket.server.ApplicationPropertiesService;
import com.atlassian.bitbucket.user.ApplicationUser;
import com.atlassian.bitbucket.util.NamedLink;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;

public enum PrnfbVariable {

 BUTTON_TRIGGER_TITLE(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction pullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return getOrEmpty(variables, BUTTON_TRIGGER_TITLE);
  }
 }), EVERYTHING_URL(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction pullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   List<String> parts = newArrayList();
   for (PrnfbVariable v : PrnfbVariable.values()) {
    if (v != EVERYTHING_URL) {
     parts.add(v.name() + "=\\${" + v.name() + "}");
    }
   }
   return on('&').join(parts);
  }
 }), INJECTION_URL_VALUE(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction pullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   if (!prnfbNotification.getInjectionUrl().isPresent()) {
    return "";
   }
   UrlInvoker urlInvoker = urlInvoker() //
     .withUrlParam(prnfbNotification.getInjectionUrl().get()) //
     .withMethod(GET)//
     .withProxyServer(prnfbNotification.getProxyServer()) //
     .withProxyPort(prnfbNotification.getProxyPort()) //
     .withProxyUser(prnfbNotification.getProxyUser()) //
     .withProxyPassword(prnfbNotification.getProxyPassword())//
     .appendBasicAuth(prnfbNotification)//
     .withClientKeyStore(clientKeyStore)//
     .shouldAcceptAnyCertificate(shouldAcceptAnyCertificate);
   createInvoker()//
     .invoke(urlInvoker);
   String rawResponse = urlInvoker.getResponseString().trim();
   if (prnfbNotification.getInjectionUrlRegexp().isPresent()) {
    Matcher m = compile(prnfbNotification.getInjectionUrlRegexp().get()).matcher(rawResponse);
    if (!m.find()) {
     return "";
    }
    if (m.groupCount() == 0) {
     return m.group();
    }
    return m.group(1);
   } else {
    return rawResponse;
   }
  }
 }), PULL_REQUEST_ACTION(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return prnfbPullRequestAction.name();
  }
 }), PULL_REQUEST_AUTHOR_DISPLAY_NAME(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getAuthor().getUser().getDisplayName();
  }
 }), PULL_REQUEST_AUTHOR_EMAIL(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getAuthor().getUser().getEmailAddress();
  }
 }), PULL_REQUEST_AUTHOR_ID(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getAuthor().getUser().getId() + "";
  }
 }), PULL_REQUEST_AUTHOR_NAME(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getAuthor().getUser().getName();
  }
 }), PULL_REQUEST_AUTHOR_SLUG(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getAuthor().getUser().getSlug();
  }
 }), PULL_REQUEST_COMMENT_TEXT(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return getOrEmpty(variables, PULL_REQUEST_COMMENT_TEXT);
  }
 }), PULL_REQUEST_FROM_BRANCH(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getFromRef().getDisplayId();
  }
 }), PULL_REQUEST_FROM_HASH(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getFromRef().getLatestCommit();
  }
 }), PULL_REQUEST_FROM_HTTP_CLONE_URL(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return cloneUrlFromRepository(http, pullRequest.getFromRef().getRepository(), repositoryService);
  }
 }), PULL_REQUEST_FROM_ID(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getFromRef().getId();
  }
 }), PULL_REQUEST_FROM_REPO_ID(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getFromRef().getRepository().getId() + "";
  }
 }), PULL_REQUEST_FROM_REPO_NAME(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getFromRef().getRepository().getName() + "";
  }
 }), PULL_REQUEST_FROM_REPO_PROJECT_ID(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getFromRef().getRepository().getProject().getId() + "";
  }
 }), PULL_REQUEST_FROM_REPO_PROJECT_KEY(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getFromRef().getRepository().getProject().getKey();
  }
 }), PULL_REQUEST_FROM_REPO_SLUG(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getFromRef().getRepository().getSlug() + "";
  }
 }), PULL_REQUEST_FROM_SSH_CLONE_URL(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return cloneUrlFromRepository(ssh, pullRequest.getFromRef().getRepository(), repositoryService);
  }
 }), PULL_REQUEST_ID(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getId() + "";
  }
 }), PULL_REQUEST_MERGE_COMMIT(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfsPullRequestAction,
    ApplicationUser stashUser, RepositoryService repositoryService, ApplicationPropertiesService propertiesService,
    PrnfbNotification prnfsNotification, Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore,
    boolean shouldAcceptAnyCertificate) {
   return getOrEmpty(variables, PULL_REQUEST_MERGE_COMMIT);
  }
 }), PULL_REQUEST_PARTICIPANTS_APPROVED_COUNT(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction pullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return Integer.toString(newArrayList(filter(pullRequest.getParticipants(), isApproved)).size());
  }
 }), PULL_REQUEST_REVIEWERS(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction pullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return iterableToString(transform(pullRequest.getReviewers(), (p) -> p.getUser().getDisplayName()));
  }
 }), PULL_REQUEST_REVIEWERS_APPROVED_COUNT(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction pullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return Integer.toString(newArrayList(filter(pullRequest.getReviewers(), isApproved)).size());
  }
 }), PULL_REQUEST_REVIEWERS_ID(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction pullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return iterableToString(transform(pullRequest.getReviewers(), (p) -> Integer.toString(p.getUser().getId())));
  }
 }), PULL_REQUEST_REVIEWERS_SLUG(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction pullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return iterableToString(transform(pullRequest.getReviewers(), (p) -> p.getUser().getSlug()));
  }

 }), PULL_REQUEST_TITLE(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction pullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getTitle();
  }
 }), PULL_REQUEST_TO_BRANCH(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getToRef().getDisplayId();
  }
 }), PULL_REQUEST_TO_HASH(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getToRef().getLatestCommit();
  }
 }), PULL_REQUEST_TO_HTTP_CLONE_URL(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return cloneUrlFromRepository(http, pullRequest.getToRef().getRepository(), repositoryService);
  }
 }), PULL_REQUEST_TO_ID(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getToRef().getId();
  }
 }), PULL_REQUEST_TO_REPO_ID(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getToRef().getRepository().getId() + "";
  }
 }), PULL_REQUEST_TO_REPO_NAME(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getToRef().getRepository().getName() + "";
  }
 }), PULL_REQUEST_TO_REPO_PROJECT_ID(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getToRef().getRepository().getProject().getId() + "";
  }
 }), PULL_REQUEST_TO_REPO_PROJECT_KEY(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getToRef().getRepository().getProject().getKey();
  }
 }), PULL_REQUEST_TO_REPO_SLUG(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getToRef().getRepository().getSlug() + "";
  }
 }), PULL_REQUEST_TO_SSH_CLONE_URL(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return cloneUrlFromRepository(ssh, pullRequest.getToRef().getRepository(), repositoryService);
  }
 }), PULL_REQUEST_URL(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return getPullRequestUrl(propertiesService, pullRequest);
  }
 }), PULL_REQUEST_USER_DISPLAY_NAME(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return applicationUser.getDisplayName();
  }
 }), PULL_REQUEST_USER_EMAIL_ADDRESS(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return applicationUser.getEmailAddress();
  }
 }), PULL_REQUEST_USER_ID(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction pullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return applicationUser.getId() + "";
  }
 }), PULL_REQUEST_USER_NAME(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction pullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return applicationUser.getName();
  }
 }), PULL_REQUEST_USER_SLUG(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction pullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return applicationUser.getSlug();
  }
 }), PULL_REQUEST_VERSION(new PrnfbVariableResolver() {
  @Override
  public String resolve(PullRequest pullRequest, PrnfbPullRequestAction prnfbPullRequestAction,
    ApplicationUser applicationUser, RepositoryService repositoryService,
    ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
    Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
   return pullRequest.getVersion() + "";
  }
 });

 private static final Predicate<PullRequestParticipant> isApproved = new Predicate<PullRequestParticipant>() {
  @Override
  public boolean apply(PullRequestParticipant input) {
   return input.isApproved();
  }
 };

 private static Invoker mockedInvoker = new Invoker() {
  @Override
  public void invoke(UrlInvoker urlInvoker) {
   urlInvoker.invoke();
  }
 };

 @VisibleForTesting
 public static void setInvoker(Invoker invoker) {
  PrnfbVariable.mockedInvoker = invoker;
 }

 private static String cloneUrlFromRepository(RepoProtocol protocol, Repository repository,
   RepositoryService repositoryService) {
  RepositoryCloneLinksRequest request = new RepositoryCloneLinksRequest.Builder().protocol(protocol.name())
    .repository(repository).build();
  final Set<NamedLink> cloneLinks = repositoryService.getCloneLinks(request);
  return cloneLinks.iterator().hasNext() ? cloneLinks.iterator().next().getHref() : "";
 }

 private static Invoker createInvoker() {
  if (mockedInvoker != null) {
   return mockedInvoker;
  }
  return new Invoker() {
   @Override
   public void invoke(UrlInvoker urlInvoker) {
    urlInvoker.invoke();
   }
  };
 }

 private static String getOrEmpty(Map<PrnfbVariable, Supplier<String>> variables, PrnfbVariable variable) {
  if (variables.get(variable) == null) {
   return "";
  }
  return variables.get(variable).get();
 }

 private static String getPullRequestUrl(ApplicationPropertiesService propertiesService, PullRequest pullRequest) {
  return propertiesService.getBaseUrl() + "/projects/" + pullRequest.getToRef().getRepository().getProject().getKey()
    + "/repos/" + pullRequest.getToRef().getRepository().getSlug() + "/pull-requests/" + pullRequest.getId();
 }

 private static String iterableToString(Iterable<String> slist) {
  List<String> sorted = usingToString().sortedCopy(slist);
  return on(',').join(sorted);
 }

 private PrnfbVariableResolver resolver;

 PrnfbVariable(PrnfbVariableResolver resolver) {
  this.resolver = resolver;
 }

 public String resolve(PullRequest pullRequest, PrnfbPullRequestAction pullRequestAction,
   ApplicationUser applicationUser, RepositoryService repositoryService,
   ApplicationPropertiesService propertiesService, PrnfbNotification prnfbNotification,
   Map<PrnfbVariable, Supplier<String>> variables, ClientKeyStore clientKeyStore, boolean shouldAcceptAnyCertificate) {
  return this.resolver.resolve(pullRequest, pullRequestAction, applicationUser, repositoryService, propertiesService,
    prnfbNotification, variables, clientKeyStore, shouldAcceptAnyCertificate);
 }
}
