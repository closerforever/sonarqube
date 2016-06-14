/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ServletFilterTest {
  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Test
  public void include_all() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.create("/*");
    assertThat(pattern.matches("/")).isTrue();
    assertThat(pattern.matches("/foo/ooo")).isTrue();

    assertThat(pattern.getIncludePatterns()).containsOnly("/*");
    assertThat(pattern.getExcludePatterns()).isEmpty();
  }

  @Test
  public void includeend_of_url() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.create("*foo");
    assertThat(pattern.matches("/")).isFalse();
    assertThat(pattern.matches("/hello/foo")).isTrue();
    assertThat(pattern.matches("/hello/bar")).isFalse();
    assertThat(pattern.matches("/foo")).isTrue();
    assertThat(pattern.matches("/foo2")).isFalse();
  }

  @Test
  public void include_beginning_of_url() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.create("/foo/*");
    assertThat(pattern.matches("/")).isFalse();
    assertThat(pattern.matches("/foo")).isTrue();
    assertThat(pattern.matches("/foo/bar")).isTrue();
    assertThat(pattern.matches("/bar")).isFalse();
  }

  @Test
  public void include_exact_url() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.create("/foo");
    assertThat(pattern.matches("/")).isFalse();
    assertThat(pattern.matches("/foo")).isTrue();
    assertThat(pattern.matches("/foo/")).isFalse();
    assertThat(pattern.matches("/bar")).isFalse();
  }

  @Test
  public void reject_all() throws Exception {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.builder()
      .setExcludePatterns("/*")
      .build();
    assertThat(pattern.matches("/")).isFalse();
    assertThat(pattern.matches("/foo/ooo")).isFalse();
  }

  @Test
  public void reject_end_of_url() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.builder()
      .setExcludePatterns("*foo")
      .build();

    assertThat(pattern.matches("/")).isTrue();
    assertThat(pattern.matches("/hello/foo")).isFalse();
    assertThat(pattern.matches("/hello/bar")).isTrue();
    assertThat(pattern.matches("/foo")).isFalse();
    assertThat(pattern.matches("/foo2")).isTrue();
  }

  @Test
  public void reject_beginning_of_url() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.builder()
      .setExcludePatterns("/foo/*")
      .build();

    assertThat(pattern.matches("/")).isTrue();
    assertThat(pattern.matches("/foo")).isFalse();
    assertThat(pattern.matches("/foo/bar")).isFalse();
    assertThat(pattern.matches("/bar")).isTrue();
  }

  @Test
  public void reject_exact_url() {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.builder()
      .setExcludePatterns("/foo")
      .build();

    assertThat(pattern.matches("/")).isTrue();
    assertThat(pattern.matches("/foo")).isFalse();
    assertThat(pattern.matches("/foo/")).isTrue();
    assertThat(pattern.matches("/bar")).isTrue();
  }

  @Test
  public void use_multiple_include_patterns() throws Exception {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.builder()
      .setIncludePatterns("/foo", "/foo2")
      .build();
    assertThat(pattern.matches("/")).isFalse();
    assertThat(pattern.matches("/foo")).isTrue();
    assertThat(pattern.matches("/foo2")).isTrue();
    assertThat(pattern.matches("/foo/")).isFalse();
    assertThat(pattern.matches("/bar")).isFalse();
  }

  @Test
  public void use_multiple_exclude_patterns() throws Exception {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.builder()
      .setExcludePatterns("/foo", "/foo2")
      .build();
    assertThat(pattern.matches("/")).isTrue();
    assertThat(pattern.matches("/foo")).isFalse();
    assertThat(pattern.matches("/foo2")).isFalse();
    assertThat(pattern.matches("/foo/")).isTrue();
    assertThat(pattern.matches("/bar")).isTrue();
  }

  @Test
  public void use_include_and_exclude_patterns() throws Exception {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.builder()
      .setIncludePatterns("/foo/*", "/foo/lo*")
      .setExcludePatterns("/foo/login", "/foo/logout", "/foo/list")
      .build();
    assertThat(pattern.matches("/")).isFalse();
    assertThat(pattern.matches("/foo")).isTrue();
    assertThat(pattern.matches("/foo/login")).isFalse();
    assertThat(pattern.matches("/foo/logout")).isFalse();
    assertThat(pattern.matches("/foo/list")).isFalse();
    assertThat(pattern.matches("/foo/locale")).isTrue();
    assertThat(pattern.matches("/foo/index")).isTrue();
  }

  @Test
  public void exclude_pattern_has_higher_priority_than_include_pattern() throws Exception {
    ServletFilter.UrlPattern pattern = ServletFilter.UrlPattern.builder()
      .setIncludePatterns("/foo")
      .setExcludePatterns("/foo")
      .build();
    assertThat(pattern.matches("/foo")).isFalse();
  }

  @Test
  public void url_pattern_cannot_contain_empty_patterns() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Empty urls");
    ServletFilter.UrlPattern.builder()
      .setExcludePatterns()
      .setIncludePatterns()
      .build();
  }

  @Test
  public void url_pattern_cant_be_empty() {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("Empty url");
    ServletFilter.UrlPattern.create("");
  }

  @Test
  public void filter_should_return_url_pattern() {
    ServletFilter filter = new FakeFilter();
    assertThat(filter.doGetPattern().getIncludePatterns()).containsOnly("/fake");
    assertThat(filter.doGetPattern().getExcludePatterns()).isEmpty();
  }

  @Test
  public void filter_should_apply_to_all_urls_by_default() {
    ServletFilter filter = new DefaultFilter();
    assertThat(filter.doGetPattern().getIncludePatterns()).containsOnly("/*");
    assertThat(filter.doGetPattern().getExcludePatterns()).isEmpty();
  }

  @Test
  public void get_url() throws Exception {
    assertThat(ServletFilter.UrlPattern.create("/*").getUrl()).isEqualTo("/*");
  }

  @Test
  public void fail_to_get_url_when_building_pattern_with_many_urls() throws Exception {
    thrown.expect(IllegalStateException.class);
    thrown.expectMessage("this method is deprecated and should not be used anymore");
    ServletFilter.UrlPattern.builder()
      .setIncludePatterns("/foo/*", "/foo/lo*")
      .setExcludePatterns("/foo/login", "/foo/logout", "/foo/list")
      .build().getUrl();
  }

  static class FakeFilter extends ServletFilter {
    @Override
    public UrlPattern doGetPattern() {
      return UrlPattern.create("/fake");
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    }

    public void destroy() {
    }
  }

  static class DefaultFilter extends ServletFilter {
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
    }

    public void destroy() {
    }
  }
}
